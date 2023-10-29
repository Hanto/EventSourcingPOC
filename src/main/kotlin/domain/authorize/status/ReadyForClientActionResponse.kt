package domain.authorize.status

import domain.authorize.events.ConfirmedEvent
import domain.authorize.events.PaymentEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.gateway.ActionType
import domain.authorize.steps.gateway.AuthorizeStatus
import domain.authorize.steps.gateway.ClientAction
import domain.authorize.steps.routing.PaymentAccount
import domain.events.*
import domain.payment.PaymentPayload

class ReadyForClientActionResponse
(
    override val newSideEffectEvents: MutableList<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: Int,
    val paymentAccount: PaymentAccount,
    val clientAction: ClientAction,

    ): PaymentStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus =

        when (event)
        {
            is ConfirmedEvent -> apply(event, isNew)
            else -> this
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: ConfirmedEvent, isNew: Boolean): PaymentStatus
    {
        return when (event.authorizeResponse.status)
        {
            is AuthorizeStatus.Success ->
            {
                addNewEvent(PaymentAuthorizedEvent, isNew)
                addNewEvent(PaymentAuthenticationCompletedEvent, isNew)

                Authorized(
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemps = retryAttemps,
                    paymentAccount = paymentAccount
                )
            }

            is AuthorizeStatus.ClientActionRequested ->
            {
                addNewEventsForClientAction(event.authorizeResponse.status.clientAction.type, isNew)

                ReadyForClientActionResponse(
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemps = retryAttemps,
                    paymentAccount = paymentAccount,
                    clientAction = event.authorizeResponse.status.clientAction
                )
            }

            is AuthorizeStatus.Reject ->
            {
                addNewEvent(AuthorizationAttemptRejectedEvent, isNew)
                addNewEvent(PaymentAuthenticationCompletedEvent, isNew)

                tryToRetry(isNew)
            }

            is AuthorizeStatus.Fail ->
            {
                addNewEvent(PaymentRejectedEvent, isNew)

                Failed(
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents,
                    reason = "exception on authorization"
                )
            }
        }
    }

    companion object { const val MAX_RETRIES = 1 }
    private fun tryToRetry(isNew: Boolean): PaymentStatus
    {
        return if (retryAttemps < MAX_RETRIES)
        {
            addNewEvent(PaymentRetriedEvent, isNew)

            ReadyForRoutingRetry(
                paymentPayload = paymentPayload,
                newSideEffectEvents = newSideEffectEvents,
                riskAssessmentOutcome = riskAssessmentOutcome,
                retryAttemps = retryAttemps + 1,
                paymentAccount = paymentAccount
            )
        }
        else
        {
            addNewEvent(PaymentRejectedEvent, isNew)

            RejectedByGateway(
                paymentPayload = paymentPayload,
                newSideEffectEvents = newSideEffectEvents,
                riskAssessmentOutcome = riskAssessmentOutcome,
                retryAttemps = retryAttemps,
                paymentAccount = paymentAccount
            )
        }
    }

    private fun addNewEventsForClientAction(actionType: ActionType, isNew: Boolean)
    {
        when (actionType)
        {
            ActionType.FINGERPRINT -> addNewEvent(BrowserFingerprintRequestedEvent, isNew)
            ActionType.REDIRECT, ActionType.CHALLENGE -> addNewEvent(UserApprovalRequestedEvent, isNew)
        }
    }

    private fun addNewEvent(event: SideEffectEvent, isNew: Boolean)
    {
        if (isNew)
            newSideEffectEvents.add(event)
    }
}
