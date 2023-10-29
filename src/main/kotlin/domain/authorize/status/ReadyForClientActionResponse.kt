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
import domain.utils.letIf

class ReadyForClientActionResponse
(
    override val baseVersion: Int,
    override val newEvents: List<PaymentEvent>,
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
        val newEvents = if (isNew) newEvents + event else newEvents
        val newVersion = if (isNew) baseVersion else event.version

        return when (event.authorizeResponse.status)
        {
            is AuthorizeStatus.Success ->
            {
                newSideEffectEvents.addNewEvent(PaymentAuthorizedEvent, isNew)
                newSideEffectEvents.addNewEvent(PaymentAuthenticationCompletedEvent, isNew)

                Authorized(
                    baseVersion = newVersion,
                    newEvents = newEvents,
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
                    baseVersion = newVersion,
                    newEvents = newEvents,
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
                        baseVersion = newVersion,
                        newEvents = newEvents,
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
                        baseVersion = newVersion,
                        newEvents = newEvents,
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
                    baseVersion = newVersion,
                    newEvents = newEvents,
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents,
                    reason = "exception on authorization"
                )
            }
        }
    }

    companion object { const val MAX_RETRIES = 1 }
    private fun MutableList<SideEffectEvent>.addNewEvent(event: SideEffectEvent, isNew: Boolean) =

        this.letIf({ isNew }, { this.add(event); this})
}
