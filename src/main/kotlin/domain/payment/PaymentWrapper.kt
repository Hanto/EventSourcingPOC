package domain.payment

import domain.events.SideEffectEvent
import domain.payment.lifecycle.events.*
import domain.payment.lifecycle.status.Payment
import domain.payment.lifecycle.status.ReadyForPaymentRequest
import domain.payment.payload.PaymentId
import domain.payment.payload.PaymentPayload
import domain.services.fraud.FraudAnalysisResult
import domain.services.gateway.AuthorizeResponse
import domain.services.routing.RoutingResult

data class PaymentWrapper
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
        if (!event.version.isSameVersion(nextVersion()))
            throw IllegalArgumentException("new event version: ${event.version} doesn't match expected new version: ${nextVersion()}")

        val newPayment = payment.apply(event, isNew = true)

        return PaymentWrapper(newPayment)
    }

    private fun nextVersion(): Version =
        getBaseVersion().nextEventVersion(getNewEvents())

    // ACTIONS:
    //------------------------------------------------------------------------------------------------------------------

    fun addPaymentPayload(paymentPayload: PaymentPayload): PaymentWrapper
    {
        val event = PaymentRequestedEvent(
            paymentId = paymentPayload.paymentId,
            version = nextVersion(),
            paymentPayload = paymentPayload)

        return applyNewEvent(event)
    }

    fun addFraudAnalysisResult(fraudAnalysisResult: FraudAnalysisResult): PaymentWrapper
    {
        val event = RiskEvaluatedEvent(
            paymentId = getPaymentPayload().paymentId,
            version = nextVersion(),
            fraudAnalysisResult = fraudAnalysisResult)

        return applyNewEvent(event)
    }

    fun addRoutingResult(routingResult: RoutingResult): PaymentWrapper
    {
        val event = RoutingEvaluatedEvent(
            paymentId = getPaymentPayload().paymentId,
            version = nextVersion(),
            routingResult = routingResult)

        return applyNewEvent(event)
    }

    fun addAuthorizeResponse(authorizeResponse: AuthorizeResponse): PaymentWrapper
    {
        val event = AuthorizationRequestedEvent(
            paymentId = getPaymentPayload().paymentId,
            version = nextVersion(),
            authorizeResponse = authorizeResponse)

        return applyNewEvent(event)
    }

    fun addConfirmParameters(confirmParameters: Map<String, Any>): PaymentWrapper
    {
        val event = ReturnedFromClientEvent(
            paymentId = getPaymentPayload().paymentId,
            version = nextVersion(),
            confirmParameters = confirmParameters)

        return applyNewEvent(event)
    }

    fun addConfirmResponse(authorizeResponse: AuthorizeResponse): PaymentWrapper
    {
        val event = ConfirmationRequestedEvent(
            paymentId = getPaymentPayload().paymentId,
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

    fun getBaseVersion(): Version =
        payment.version

    fun getPaymentId(): PaymentId =
        payment.payload().paymentId

    fun getPaymentPayload(): PaymentPayload =
        payment.payload()
}
