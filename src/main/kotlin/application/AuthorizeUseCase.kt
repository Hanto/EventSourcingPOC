package application

import domain.authorize.status.*
import domain.authorize.steps.fraud.RiskAssessmentService
import domain.authorize.steps.gateway.AuthorizationGateway
import domain.authorize.steps.routing.RoutingService
import domain.events.EventPublisher
import domain.payment.PaymentPayload
import domain.payment.PaymentWrapper
import domain.payment.payload.PaymentId
import domain.repositories.PaymentRepository
import domain.utils.letIf

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
        return ReadyForPaymentRequest().addPaymentPayload(paymentPayload)
            .letIf { it: ReadyForRisk -> it.addFraudAnalysisResult(riskService.assessRisk(it)) }
            .letIf { it: ReadyForRouting -> tryToAuthorize(it) }
            .let { paymentRepository.save(it) }
            .also { sendSideEffectEvents(it) }
    }

    private fun tryToAuthorize(input: Routed): Payment
    {
        return input.addRoutingResult(routingService.routeForPayment(input))
            .letIf { it: ReadyForAuthorization -> it.addAuthorizeResponse(authorizeService.authorize(it)) }
            .letIf { it: ReadyForRoutingRetry -> tryToAuthorize(it) }
    }

    fun confirm(paymentId: PaymentId, confirmParams: Map<String, Any>): Payment
    {
        return paymentRepository.load(paymentId)!!
            .letIf { it: ReadyForClientActionResponse -> it.addConfirmParameters(confirmParams) }
            .letIf { it: ReadyForConfirm -> it.addConfirmResponse(authorizeService.confirm(it)) }
            .letIf { it: ReadyForRoutingRetry -> tryToAuthorize(it) }
            .let { paymentRepository.save(it) }
            .also { sendSideEffectEvents(it) }
    }

    private fun sendSideEffectEvents(payment: Payment)
    {
        payment.sideEffectEvents.forEach { eventPublisher.publish(it) }
        payment.flushSideEffectEvents()
    }

    // HELPER:
    //------------------------------------------------------------------------------------------------------------------

    private inline fun <reified T> PaymentWrapper.letIf(function: (T, PaymentWrapper) -> PaymentWrapper): PaymentWrapper =

        if (this.payment is T)
            function.invoke(this as T, this)
        else this

    // WITH WRAPPER:
    //------------------------------------------------------------------------------------------------------------------

    fun authorize2(paymentPayload: PaymentPayload): PaymentWrapper
    {
        return PaymentWrapper().addPaymentPayload(paymentPayload)
            .letIf { it: ReadyForRisk, p -> p.addFraudAnalysisResult(riskService.assessRisk(it)) }
            .letIf {  _: ReadyForRouting, p -> tryToAuthorize2(p) }
    }

    private fun tryToAuthorize2(input: PaymentWrapper): PaymentWrapper
    {
        return input
            .letIf { it: Routed, p -> p.addRoutingResult(routingService.routeForPayment(it)) }
            .letIf { it: ReadyForAuthorization, p -> p.addAuthorizeResponse(authorizeService.authorize(it)) }
            .letIf {  _: ReadyForRoutingRetry, p -> tryToAuthorize2(p) }
    }
}
