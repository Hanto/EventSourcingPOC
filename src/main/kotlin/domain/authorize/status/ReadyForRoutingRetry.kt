package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.RoutingEvaluatedEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.routing.PaymentAccount
import domain.authorize.steps.routing.RoutingResult
import domain.events.PaymentFailedEvent
import domain.events.PaymentRejectedEvent
import domain.events.RoutingCompletedEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload

class ReadyForRoutingRetry
(
    override val newSideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: Int,
    val paymentAccount: PaymentAccount

) : PaymentStatus
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
                    newSideEffectEvents = newSideEffectEvents)
            }

            is RoutingResult.Proceed ->
            {
                newSideEffectEvents.addNewEvent(RoutingCompletedEvent, isNew)

                if (event.routingResult.account == paymentAccount)
                {
                    newSideEffectEvents.addNewEvent(PaymentRejectedEvent, isNew)

                    RejectedByGateway(
                        paymentPayload = paymentPayload,
                        newSideEffectEvents = newSideEffectEvents,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        retryAttemps = retryAttemps,
                        paymentAccount = event.routingResult.account
                    )
                }

                else
                    ReadyForAuthorization(
                        paymentPayload = paymentPayload,
                        newSideEffectEvents = newSideEffectEvents,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        retryAttemps = retryAttemps,
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
