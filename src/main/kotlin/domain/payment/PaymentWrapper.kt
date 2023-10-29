package domain.payment

import domain.authorize.events.*
import domain.authorize.status.Payment
import domain.authorize.status.ReadyForPaymentRequest
import domain.authorize.steps.fraud.FraudAnalysisResult
import domain.authorize.steps.gateway.AuthorizeResponse
import domain.authorize.steps.routing.RoutingResult
import domain.events.SideEffectEvent

class PaymentWrapper
(
    val payment: Payment = ReadyForPaymentRequest(),
)
{
    // CONSTRUCTOR:
    //------------------------------------------------------------------------------------------------------------------

    fun applyRecordedEvent(event: PaymentEvent): PaymentWrapper
    {
        val newPayment = payment.apply(event, isNew = false)

        return PaymentWrapper(newPayment)
    }

    private fun applyNewEvent(event: PaymentEvent): PaymentWrapper
    {
        if (event.version != nextVersion())
            throw IllegalArgumentException("new event version: ${event.version} doesn't match expected new version: ${nextVersion()}")

        val newPayment = payment.apply(event, isNew = true)

        return PaymentWrapper(newPayment)
    }

    private fun nextVersion(): Int =
        getBaseVersion() + getNewEvents().size + 1

    // ACTIONS:
    //------------------------------------------------------------------------------------------------------------------

    fun addPaymentPayload(paymentPayload: PaymentPayload): PaymentWrapper
    {
        val event = PaymentRequestedEvent(
            version = nextVersion(),
            paymentPayload = paymentPayload)

        return applyNewEvent(event)
    }

    fun addFraudAnalysisResult(fraudAnalysisResult: FraudAnalysisResult): PaymentWrapper
    {
        val event = RiskEvaluatedEvent(
            version = nextVersion(),
            fraudAnalysisResult = fraudAnalysisResult)

        return applyNewEvent(event)
    }

    fun addRoutingResult(routingResult: RoutingResult): PaymentWrapper
    {
        val event = RoutingEvaluatedEvent(
            version = nextVersion(),
            routingResult = routingResult)

        return applyNewEvent(event)
    }

    fun addAuthorizeResponse(authorizeResponse: AuthorizeResponse): PaymentWrapper
    {
        val event = AuthorizationRequestedEvent(
            version = nextVersion(),
            authorizeResponse = authorizeResponse)

        return applyNewEvent(event)
    }

    fun addConfirmParameters(confirmParameters: Map<String, Any>): PaymentWrapper
    {
        val event = ReturnedFromClient(
            version = nextVersion(),
            confirmParameters = confirmParameters)

        return applyNewEvent(event)
    }

    fun addConfirmResponse(authorizeResponse: AuthorizeResponse): PaymentWrapper
    {
        val event = ConfirmationRequestedEvent(
            version = nextVersion(),
            authorizeResponse = authorizeResponse)

        return applyNewEvent(event)
    }

    // EVENTS:
    //------------------------------------------------------------------------------------------------------------------

    fun getNewEvents(): List<PaymentEvent> =
        payment.paymentEvents

    fun getNewSideEffects(): List<SideEffectEvent> =
        payment.sideEffectEvents

    // MISC:
    //------------------------------------------------------------------------------------------------------------------

    fun getBaseVersion(): Int =
        payment.baseVersion

    fun getPaymentId(): PaymentId =
        payment.paymentPayload?.paymentId!!

    fun getPaymentPayload(): PaymentPayload =
        payment.paymentPayload!!
}
