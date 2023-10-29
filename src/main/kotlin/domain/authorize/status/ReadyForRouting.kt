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

class ReadyForRouting
(
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

        return when (event.routingResult)
        {
            is RoutingResult.RoutingError ->
            {
                newSideEffectEvents.addNewEvent(RoutingCompletedEvent, isNew)
                newSideEffectEvents.addNewEvent(PaymentFailedEvent, isNew)

                when (event.routingResult)
                {
                    is RoutingResult.RoutingError.InvalidCurrency -> Failed(
                        paymentPayload = paymentPayload,
                        newSideEffectEvents = newSideEffectEvents,
                        reason = "Currency not accepted")

                    is RoutingResult.RoutingError.BankAccountNotFound -> Failed(
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
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents
                )
            }

            is RoutingResult.Proceed ->
            {
                newSideEffectEvents.addNewEvent(RoutingCompletedEvent, isNew)

                ReadyForAuthorization(
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemps = 0,
                    paymentAccount = event.routingResult.account
                )
            }
        }
    }

    private fun MutableList<SideEffectEvent>.addNewEvent(event: SideEffectEvent, isNew: Boolean)
    {
        if (isNew)
            this.add(event)
    }
}
