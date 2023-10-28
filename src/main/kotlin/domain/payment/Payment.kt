package domain.payment

import domain.authorize.events.*
import domain.authorize.status.AuthorizationStatus
import domain.authorize.status.ReadyForPaymentRequest
import domain.authorize.steps.fraud.FraudAnalysisResult
import domain.authorize.steps.gateway.AuthorizeResponse
import domain.authorize.steps.routing.RoutingResult
import domain.sideeffectevents.SideEffectEvent

class Payment
{
    private val newEvents: MutableList<PaymentEvent> = mutableListOf()
    var authorizationStatus: AuthorizationStatus = ReadyForPaymentRequest()
        private set
    var baseVersion = 0
        private set

    constructor(paymentPayload: PaymentPayload)
    {
        val event = PaymentRequestedEvent(
            version = nextVersion(),
            paymentPayload = paymentPayload)

        applyNewEvent(event)
    }

    constructor(events: List<PaymentEvent>)
    {
        events
            .sortedBy { it.version }
            .forEach { applyRecordedEvent(it) }
    }

    private fun applyRecordedEvent(event: PaymentEvent)
    {
        authorizationStatus = authorizationStatus.apply(event, isNew = false)
        baseVersion = event.version
    }

    private fun applyNewEvent(event: PaymentEvent)
    {
        if (event.version != nextVersion())
            throw IllegalArgumentException("new event version: ${event.version} doesn't match expected new version: ${nextVersion()}")

        authorizationStatus = authorizationStatus.apply(event, isNew = true)
        newEvents.add(event)
    }

    private fun nextVersion(): Int =
        baseVersion + newEvents.size + 1

    // MAIN:
    //------------------------------------------------------------------------------------------------------------------

    fun addFraudAnalysisResult(fraudAnalysisResult: FraudAnalysisResult)
    {
        val event = RiskEvaluatedEvent(
            version = nextVersion(),
            fraudAnalysisResult = fraudAnalysisResult)

        applyNewEvent(event)
    }

    fun addRoutingResult(routingResult: RoutingResult)
    {
        val event = RoutingEvaluatedEvent(
            version = nextVersion(),
            routingResult = routingResult)

        applyNewEvent(event)
    }

    fun addAuthorizeResponse(authorizeResponse: AuthorizeResponse)
    {
        val event = AuthorizationRequestedEvent(
            version = nextVersion(),
            authorizeResponse = authorizeResponse)

        applyNewEvent(event)
    }

    fun addConfirmResponse(authorizeResponse: AuthorizeResponse, confirmParameters: Map<String, Any>)
    {
        val event = ConfirmedEvent(
            version = nextVersion(),
            authorizeResponse = authorizeResponse,
            confirmParameters = confirmParameters)

        applyNewEvent(event)
    }

    // EVENTS:
    //------------------------------------------------------------------------------------------------------------------

    fun getNewEvents(): List<PaymentEvent> =
        newEvents.toList()

    fun getNewSideEffects(): List<SideEffectEvent> =
        authorizationStatus.newEvents.toList()

    // MISC:
    //------------------------------------------------------------------------------------------------------------------

    fun getPaymentPayload(): PaymentPayload =
        authorizationStatus.paymentPayload!!
}
