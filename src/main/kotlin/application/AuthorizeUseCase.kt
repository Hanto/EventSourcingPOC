package application

import domain.payment.data.paymentpayload.PaymentId
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.sideeffectevents.EventPublisher
import domain.payment.state.*
import domain.repositories.PaymentRepositoryNew
import domain.repositories.PaymentRepositoryOld
import domain.services.fraud.RiskAssessmentService
import domain.services.gateway.AuthorizationGateway
import domain.services.routing.RoutingService

class AuthorizeUseCase
(
    private val riskService: RiskAssessmentService,
    private val routingService: RoutingService,
    private val authorizeService: AuthorizationGateway,
    private val paymentRepositoryNew: PaymentRepositoryNew,
    private val paymentRepositoryOld: PaymentRepositoryOld,
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

    private fun tryToAuthorize(payment: ReadyForRouting): Payment
    {
        return payment
            .letIfAndSave { it: ReadyForRouting -> it.addRoutingResult(routingService.routeForPayment(payment)) }
            .letIfAndSave { it: ReadyForAuthorization -> it.addAuthorizeResponse(authorizeService.authorize(it)) }
            .letIfAndSave { it: RejectedByGateway -> it.prepareForRetry() }
            .letIf { it: ReadyForRoutingRetry -> tryToAuthorize(it) }
    }

    fun confirm(paymentId: PaymentId, confirmParams: Map<String, Any>): Payment
    {
        return paymentRepositoryNew.load(paymentId)!!
            .letIfAndSave { it: ReadyForClientActionResponse -> it.addConfirmParameters(confirmParams) }
            .letIfAndSave { it: ReadyForConfirm -> it.addConfirmResponse(authorizeService.confirm(it)) }
            .letIfAndSave { it: RejectedByGateway -> it.prepareForRetry() }
            .letIf { it: ReadyForRoutingRetry -> tryToAuthorize(it) }
    }

    private fun saveAndSendEvents(payment: Payment): Payment
    {
        payment.sideEffectEvents.forEach { eventPublisher.publish(it) }
        return paymentRepositoryNew.save(payment)
            .flushSideEffectEvents()
            .flushPaymentEvents()
            .also { paymentRepositoryOld.save(it) }
    }

    // HELPER:
    //------------------------------------------------------------------------------------------------------------------

    private inline fun <reified T, R>R.letIf(function: (T) -> R): R =

        if (this is T) function.invoke(this as T) else this

    private inline fun <reified T> Payment.letIfAndSave(function: (T) -> Payment): Payment =

        if (this is T) saveAndSendEvents(function.invoke(this)) else this
}
