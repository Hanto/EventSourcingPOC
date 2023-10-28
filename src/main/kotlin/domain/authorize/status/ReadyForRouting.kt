package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.RoutingEvaluatedEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.routing.RoutingResult
import domain.payment.PaymentPayload
import domain.sideeffectevents.PaymentFailedEvent
import domain.sideeffectevents.PaymentRejectedEvent
import domain.sideeffectevents.SideEffectEvent

class ReadyForRouting
(
    override val newEvents: MutableList<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,

): AuthorizationStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): AuthorizationStatus =

        when (event)
        {
            is RoutingEvaluatedEvent -> apply(event, isNew)
            else -> this
        }

    // ROUTING EVALUATED:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: RoutingEvaluatedEvent, isNew: Boolean): AuthorizationStatus =

        when (event.routingResult)
        {
            is RoutingResult.RoutingError ->
            {
                addNewEvent(PaymentFailedEvent, isNew)

                failedDueToRoutingError(event.routingResult, isNew)
            }

            is RoutingResult.Reject ->
            {
                addNewEvent(PaymentRejectedEvent, isNew)

                RejectedByRouting(
                    paymentPayload = paymentPayload,
                    newEvents = newEvents
                )
            }

            is RoutingResult.Proceed ->
            {
                ReadyForAuthorization(
                    paymentPayload = paymentPayload,
                    newEvents = newEvents,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemps = 0,
                    paymentAccount = event.routingResult.account
                )
            }
        }

    private fun failedDueToRoutingError(event: RoutingResult.RoutingError, isNew: Boolean): AuthorizationStatus =

        when (event)
        {
            is RoutingResult.RoutingError.InvalidCurrency -> Failed(
                paymentPayload = paymentPayload,
                newEvents = newEvents,
                reason = "Currency not accepted")

            is RoutingResult.RoutingError.BankAccountNotFound -> Failed(
                paymentPayload = paymentPayload,
                newEvents = newEvents,
                reason = "Unable to find bank account")
        }

    private fun addNewEvent(event: SideEffectEvent, isNew: Boolean)
    {
        if (isNew)
            newEvents.add(event)
    }
}
