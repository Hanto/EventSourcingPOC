package application

import domain.events.EventPublisher
import domain.payment.PaymentWrapper
import domain.payment.lifecycle.status.*
import domain.payment.payload.PaymentId
import domain.payment.payload.PaymentPayload
import domain.repositories.PaymentRepository
import domain.services.fraud.RiskAssessmentService
import domain.services.gateway.AuthorizationGateway
import domain.services.routing.RoutingService

class AuthorizeUseCase
(
    private val riskService: RiskAssessmentService,
    private val routingService: RoutingService,
    private val authorizeService: AuthorizationGateway,
    private val paymentRepository: PaymentRepository,
    private val eventPublisher: EventPublisher
)
{
    fun authorize(paymentPayload: PaymentPayload): Payment
    {
        return ReadyForPaymentRequest()
            .letIfAndSave { it: ReadyForPaymentRequest -> it.addPaymentPayload(paymentPayload) }
            .letIfAndSave { it: ReadyForRisk -> it.addFraudAnalysisResult(riskService.assessRisk(it)) }
            .letIf { it: ReadyForRoutingInitial -> tryToAuthorize(it) }
    }

    private fun tryToAuthorize(input: ReadyForRouting): Payment
    {
        return input
            .letIfAndSave { it: ReadyForRouting -> it.addRoutingResult(routingService.routeForPayment(input)) }
            .letIfAndSave { it: ReadyForAuthorization -> it.addAuthorizeResponse(authorizeService.authorize(it)) }
            .letIfAndSave { it: RejectedByGateway -> it.prepareForRetry() }
            .letIf { it: ReadyForRoutingRetry -> tryToAuthorize(it) }
    }

    fun confirm(paymentId: PaymentId, confirmParams: Map<String, Any>): Payment
    {
        return paymentRepository.load(paymentId)!!
            .letIfAndSave { it: ReadyForClientActionResponse -> it.addConfirmParameters(confirmParams) }
            .letIfAndSave { it: ReadyForConfirm -> it.addConfirmResponse(authorizeService.confirm(it)) }
            .letIfAndSave { it: RejectedByGateway -> it.prepareForRetry() }
            .letIf { it: ReadyForRoutingRetry -> tryToAuthorize(it) }
    }

    private fun saveAndSendEvents(payment: Payment): Payment
    {
        payment.sideEffectEvents.forEach { eventPublisher.publish(it) }
        return paymentRepository.save(payment)
            .flushSideEffectEvents()
            .flushPaymentEvents()
    }

    // HELPER:
    //------------------------------------------------------------------------------------------------------------------

    private inline fun <reified T, R>R.letIf(function: (T) -> R): R =

        if (this is T) function.invoke(this as T) else this

    private inline fun <reified T>Payment.letIfAndSave(function: (T) -> Payment): Payment =

        if (this is T) saveAndSendEvents(function.invoke(this)) else this

    private inline fun <reified T> PaymentWrapper.letIf(function: (T, PaymentWrapper) -> PaymentWrapper): PaymentWrapper =

        if (this.payment is T) function.invoke(this as T, this) else this

    // WITH WRAPPER:
    //------------------------------------------------------------------------------------------------------------------

    fun reAuthorize(paymentId: PaymentId): Payment
    {
        return paymentRepository.load(paymentId)!!.let { payment ->

            when (payment)
            {
                is RejectedByGateway ->
                    payment.prepareForRetry()
                        .letIf { it: ReadyForRoutingRetry -> tryToAuthorize(it) }

                is ReadyForRisk ->
                    payment.addFraudAnalysisResult(riskService.assessRisk(payment))
                        .letIf { it: ReadyForRoutingInitial -> tryToAuthorize(it) }

                is ReadyForAuthorization ->
                    payment.addAuthorizeResponse(authorizeService.authorize(payment))
                        .letIf { it: RejectedByGateway -> it.prepareForRetry() }
                        .letIf { it: ReadyForRoutingRetry -> tryToAuthorize(it) }

                is ReadyForConfirm ->
                    payment.addConfirmResponse(authorizeService.confirm(payment))
                        .letIf { it: RejectedByGateway -> it.prepareForRetry() }
                        .letIf { it: ReadyForRoutingRetry -> tryToAuthorize(it) }

                is ReadyForRoutingInitial -> tryToAuthorize(payment)
                is ReadyForRoutingRetry -> tryToAuthorize(payment)

                is ReadyForClientActionResponse -> payment
                is ReadyForPaymentRequest -> payment
                is AuthorizeEnded -> payment
            }
                .let { paymentRepository.save(it) }
                .also { saveAndSendEvents(it) }
        }
    }

    fun authorize2(paymentPayload: PaymentPayload): PaymentWrapper
    {
        return PaymentWrapper().addPaymentPayload(paymentPayload)
            .letIf { it: ReadyForRisk, p -> p.addFraudAnalysisResult(riskService.assessRisk(it)) }
            .letIf { _: ReadyForRoutingInitial, p -> tryToAuthorize2(p) }
    }

    private fun tryToAuthorize2(input: PaymentWrapper): PaymentWrapper
    {
        return input
            .letIf { it: ReadyForRouting, p -> p.addRoutingResult(routingService.routeForPayment(it)) }
            .letIf { it: ReadyForAuthorization, p -> p.addAuthorizeResponse(authorizeService.authorize(it)) }
            .letIf { _: ReadyForRoutingRetry, p -> tryToAuthorize2(p) }
    }
}
