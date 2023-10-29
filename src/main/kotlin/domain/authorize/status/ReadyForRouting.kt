package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.RoutingEvaluatedEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.routing.RoutingResult
import domain.events.PaymentFailedEvent
import domain.events.PaymentRejectedEvent
import domain.events.RoutingCompletedEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import java.util.logging.Logger

class ReadyForRouting
(
    override val baseVersion: Int,
    override val newEvents: List<PaymentEvent>,
    override val newSideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    override val riskAssessmentOutcome: RiskAssessmentOutcome,

): PaymentStatus, ReadyForAnyRouting
{
    private val log = Logger.getLogger(ReadyForRouting::class.java.name)

    override fun addRoutingResult(routingResult: RoutingResult): PaymentStatus
    {
        val event = RoutingEvaluatedEvent(
            version = baseVersion + newEvents.size + 1,
            routingResult = routingResult)

        return apply(event, isNew = true)
    }

    override fun applyRecordedEvent(event: PaymentEvent): PaymentStatus =

        apply(event, isNew = false)

    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus =

        when (event)
        {
            is RoutingEvaluatedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: RoutingEvaluatedEvent, isNew: Boolean): PaymentStatus
    {
        val newSideEffectEvents = newSideEffectEvents.toMutableList()
        val newEvents = if (isNew) newEvents + event else newEvents
        val newVersion = if (isNew) baseVersion else event.version

        return when (event.routingResult)
        {
            is RoutingResult.RoutingError ->
            {
                newSideEffectEvents.addNewEvent(PaymentFailedEvent, isNew)

                Failed(
                    baseVersion = newVersion,
                    newEvents = newEvents,
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents,
                    reason = createRoutingErrorReason(event.routingResult))
            }

            is RoutingResult.Reject ->
            {
                newSideEffectEvents.addNewEvent(RoutingCompletedEvent, isNew)
                newSideEffectEvents.addNewEvent(PaymentRejectedEvent, isNew)

                RejectedByRouting(
                    baseVersion = newVersion,
                    newEvents = newEvents,
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents
                )
            }

            is RoutingResult.Proceed ->
            {
                newSideEffectEvents.addNewEvent(RoutingCompletedEvent, isNew)

                ReadyForAuthorization(
                    baseVersion = newVersion,
                    newEvents = newEvents,
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemps = 0,
                    paymentAccount = event.routingResult.account
                )
            }
        }
    }

    private fun createRoutingErrorReason(routingError: RoutingResult.RoutingError): String =

        when (routingError)
        {
            is RoutingResult.RoutingError.InvalidCurrency -> "Currency not accepted"
            is RoutingResult.RoutingError.BankAccountNotFound -> "Unable to find bank account"
        }

    private fun MutableList<SideEffectEvent>.addNewEvent(event: SideEffectEvent, isNew: Boolean)
    {
        if (isNew)
            this.add(event)
    }
}
