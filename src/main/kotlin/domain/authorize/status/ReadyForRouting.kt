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
import domain.utils.letIf

class ReadyForRouting
(
    override val baseVersion: Int,
    override val newEvents: List<PaymentEvent>,
    override val newSideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,

): PaymentStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus =

        when (event)
        {
            is RoutingEvaluatedEvent -> apply(event, isNew)
            else -> this
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
                newSideEffectEvents.addNewEvent(RoutingCompletedEvent, isNew)
                newSideEffectEvents.addNewEvent(PaymentFailedEvent, isNew)

                when (event.routingResult)
                {
                    is RoutingResult.RoutingError.InvalidCurrency -> Failed(
                        baseVersion = newVersion,
                        newEvents = newEvents,
                        paymentPayload = paymentPayload,
                        newSideEffectEvents = newSideEffectEvents,
                        reason = "Currency not accepted")

                    is RoutingResult.RoutingError.BankAccountNotFound -> Failed(
                        baseVersion = newVersion,
                        newEvents = newEvents,
                        paymentPayload = paymentPayload,
                        newSideEffectEvents = newSideEffectEvents,
                        reason = "Unable to find bank account")
                }
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

    private fun MutableList<SideEffectEvent>.addNewEvent(event: SideEffectEvent, isNew: Boolean) =

        this.letIf({ isNew }, { this.add(event); this})
}
