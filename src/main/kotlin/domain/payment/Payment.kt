package domain.payment

import domain.authorize.events.*
import domain.authorize.status.AuthorizationStatus
import domain.authorize.status.ReadyForPaymentRequest
import domain.authorize.steps.fraud.FraudAnalysisResult
import domain.authorize.steps.gateway.AuthorizeResponse
import domain.authorize.steps.routing.RoutingResult
import domain.sideeffectevents.SideEffectEvent

class Payment
(
    private val newEvents: MutableList<PaymentEvent> = mutableListOf(),
    val authorizationStatus: AuthorizationStatus = ReadyForPaymentRequest(),
    val baseVersion: Int = 0
)
{
    // CONSTRUCTOR:
    //------------------------------------------------------------------------------------------------------------------

    fun applyRecordedEvent(event: PaymentEvent): Payment
    {
        val newAuthorizationStatus = authorizationStatus.apply(event, isNew = false)
        val newBaseVersion = event.version

        return Payment(newEvents, newAuthorizationStatus, newBaseVersion)
    }

    private fun applyNewEvent(event: PaymentEvent): Payment
    {
        if (event.version != nextVersion())
            throw IllegalArgumentException("new event version: ${event.version} doesn't match expected new version: ${nextVersion()}")

        val newAuthorizationStatus = authorizationStatus.apply(event, isNew = true)
        newEvents.add(event)

        return Payment(newEvents, newAuthorizationStatus, baseVersion)
    }

    private fun nextVersion(): Int =
        baseVersion + newEvents.size + 1

    // ACTIONS:
    //------------------------------------------------------------------------------------------------------------------

    fun addPaymentPayload(paymentPayload: PaymentPayload): Payment
    {
        val event = PaymentRequestedEvent(
            version = nextVersion(),
            paymentPayload = paymentPayload)

        return applyNewEvent(event)
    }

    fun addFraudAnalysisResult(fraudAnalysisResult: FraudAnalysisResult): Payment
    {
        val event = RiskEvaluatedEvent(
            version = nextVersion(),
            fraudAnalysisResult = fraudAnalysisResult)

        return applyNewEvent(event)
    }

    fun addRoutingResult(routingResult: RoutingResult): Payment
    {
        val event = RoutingEvaluatedEvent(
            version = nextVersion(),
            routingResult = routingResult)

        return applyNewEvent(event)
    }

    fun addAuthorizeResponse(authorizeResponse: AuthorizeResponse): Payment
    {
        val event = AuthorizationRequestedEvent(
            version = nextVersion(),
            authorizeResponse = authorizeResponse)

        return applyNewEvent(event)
    }

    fun addConfirmResponse(authorizeResponse: AuthorizeResponse, confirmParameters: Map<String, Any>): Payment
    {
        val event = ConfirmedEvent(
            version = nextVersion(),
            authorizeResponse = authorizeResponse,
            confirmParameters = confirmParameters)

        return applyNewEvent(event)
    }

    // EVENTS:
    //------------------------------------------------------------------------------------------------------------------

    fun getNewEvents(): List<PaymentEvent> =
        newEvents.toList()

    fun getNewSideEffects(): List<SideEffectEvent> =
        authorizationStatus.newSideEffectEvents.toList()

    // MISC:
    //------------------------------------------------------------------------------------------------------------------

    fun getPaymentId(): PaymentId =
        authorizationStatus.paymentPayload?.paymentId!!

    fun getPaymentPayload(): PaymentPayload =
        authorizationStatus.paymentPayload!!
}
