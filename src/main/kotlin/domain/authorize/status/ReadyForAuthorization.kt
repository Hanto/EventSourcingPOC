package domain.authorize.status

import domain.authorize.events.AuthorizationRequestedEvent
import domain.authorize.events.PaymentEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.gateway.ActionType
import domain.authorize.steps.gateway.AuthorizeStatus
import domain.authorize.steps.routing.PaymentAccount
import domain.payment.PaymentPayload
import domain.sideeffectevents.*

class ReadyForAuthorization
(
    override val newEvents: MutableList<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: Int,
    val paymentAccount: PaymentAccount

): AuthorizationStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): AuthorizationStatus =

        when (event)
        {
            is AuthorizationRequestedEvent -> apply(event, isNew)
            else -> this
        }

    // AUTHORIZATION EVALUATED:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: AuthorizationRequestedEvent, isNew: Boolean): AuthorizationStatus
    {
        addNewEvent(AuthorizationAttemptRequestedEvent, isNew)

        return when (event.authorizeResponse.status)
        {
            is AuthorizeStatus.Success ->
            {
                addNewEvent(PaymentAuthorizedEvent, isNew)

                Authorized(
                    paymentPayload = paymentPayload,
                    newEvents = newEvents,
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
                    newEvents = newEvents,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemps = retryAttemps,
                    paymentAccount = paymentAccount,
                    clientAction = event.authorizeResponse.status.clientAction
                )
            }

            is AuthorizeStatus.Reject ->
            {
                addNewEvent(AuthorizationAttemptRejectedEvent, isNew)

                return if (retryAttemps < 1)
                {
                    addNewEvent(PaymentRetriedEvent, isNew)

                    ReadyForRoutingRetry(
                        paymentPayload = paymentPayload,
                        newEvents = newEvents,
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
                        newEvents = newEvents,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        retryAttemps = retryAttemps,
                        paymentAccount = paymentAccount
                    )
                }
            }

            is AuthorizeStatus.Fail ->
            {
                addNewEvent(PaymentRejectedEvent, isNew)

                Failed(
                    paymentPayload = paymentPayload,
                    newEvents = newEvents,
                    reason = "exception on authorization"
                )
            }
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
            newEvents.add(event)
    }
}
