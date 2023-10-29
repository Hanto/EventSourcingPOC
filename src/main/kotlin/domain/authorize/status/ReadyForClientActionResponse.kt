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
    override val newSideEffectEvents: List<SideEffectEvent>,
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
        val newSideEffectEvents = newSideEffectEvents.toMutableList()

        return when (event.authorizeResponse.status)
        {
            is AuthorizeStatus.Success ->
            {
                newSideEffectEvents.addNewEvent(PaymentAuthorizedEvent, isNew)
                newSideEffectEvents.addNewEvent(PaymentAuthenticationCompletedEvent, isNew)

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
                when (event.authorizeResponse.status.clientAction.type)
                {
                    ActionType.FINGERPRINT -> newSideEffectEvents.addNewEvent(BrowserFingerprintRequestedEvent, isNew)
                    ActionType.REDIRECT, ActionType.CHALLENGE -> newSideEffectEvents.addNewEvent(UserApprovalRequestedEvent, isNew)
                }

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
                newSideEffectEvents.addNewEvent(AuthorizationAttemptRejectedEvent, isNew)
                newSideEffectEvents.addNewEvent(PaymentAuthenticationCompletedEvent, isNew)

                if (retryAttemps < MAX_RETRIES)
                {
                    newSideEffectEvents.addNewEvent(PaymentRetriedEvent, isNew)

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
                    newSideEffectEvents.addNewEvent(PaymentRejectedEvent, isNew)

                    RejectedByGateway(
                        paymentPayload = paymentPayload,
                        newSideEffectEvents = newSideEffectEvents,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        retryAttemps = retryAttemps,
                        paymentAccount = paymentAccount
                    )
                }
            }

            is AuthorizeStatus.Fail ->
            {
                newSideEffectEvents.addNewEvent(PaymentRejectedEvent, isNew)

                Failed(
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents,
                    reason = "exception on authorization"
                )
            }
        }
    }

    companion object { const val MAX_RETRIES = 1 }

    private fun MutableList<SideEffectEvent>.addNewEvent(event: SideEffectEvent, isNew: Boolean)
    {
        if (isNew)
            this.add(event)
    }
}
