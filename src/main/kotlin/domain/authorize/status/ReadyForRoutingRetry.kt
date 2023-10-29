package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.RoutingEvaluatedEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.routing.PaymentAccount
import domain.authorize.steps.routing.RoutingResult
import domain.payment.PaymentPayload
import domain.sideeffectevents.PaymentFailedEvent
import domain.sideeffectevents.PaymentRejectedEvent
import domain.sideeffectevents.RoutingCompletedEvent
import domain.sideeffectevents.SideEffectEvent

class ReadyForRoutingRetry
(
    override val newSideEffectEvents: MutableList<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: Int,
    val paymentAccount: PaymentAccount

) : AuthorizationStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): AuthorizationStatus =

        when (event)
        {
            is RoutingEvaluatedEvent -> apply(event, isNew)
            else -> this
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: RoutingEvaluatedEvent, isNew: Boolean): AuthorizationStatus =

        when (event.routingResult)
        {
            is RoutingResult.RoutingError ->
            {
                addNewEvent(RoutingCompletedEvent, isNew)
                addNewEvent(PaymentFailedEvent, isNew)

                failedDueToRoutingError(event.routingResult, isNew)
            }

            is RoutingResult.Reject ->
            {
                addNewEvent(RoutingCompletedEvent, isNew)
                addNewEvent(PaymentRejectedEvent, isNew)

                RejectedByRouting(
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents)
            }

            is RoutingResult.Proceed ->
            {
                addNewEvent(RoutingCompletedEvent, isNew)

                retryIfDifferentAccount(event.routingResult, isNew)
            }
        }

    private fun retryIfDifferentAccount(event: RoutingResult.Proceed, isNew: Boolean): AuthorizationStatus =

        if (event.account == paymentAccount)
        {
            addNewEvent(PaymentRejectedEvent, isNew)

            RejectedByGateway(
                paymentPayload = paymentPayload,
                newSideEffectEvents = newSideEffectEvents,
                riskAssessmentOutcome = riskAssessmentOutcome,
                retryAttemps = retryAttemps,
                paymentAccount = event.account
            )
        }

        else
            ReadyForAuthorization(
                paymentPayload = paymentPayload,
                newSideEffectEvents = newSideEffectEvents,
                riskAssessmentOutcome = riskAssessmentOutcome,
                retryAttemps = retryAttemps,
                paymentAccount = event.account
            )

    private fun failedDueToRoutingError(event: RoutingResult.RoutingError, isNew: Boolean): AuthorizationStatus =

        when (event)
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

    private fun addNewEvent(event: SideEffectEvent, isNew: Boolean)
    {
        if (isNew)
            newSideEffectEvents.add(event)
    }
}
