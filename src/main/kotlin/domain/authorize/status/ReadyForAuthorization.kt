package domain.authorize.status

import domain.authorize.events.AuthorizationRequestedEvent
import domain.authorize.events.PaymentEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.gateway.ActionType
import domain.authorize.steps.gateway.AuthorizeStatus
import domain.authorize.steps.routing.PaymentAccount
import domain.events.*
import domain.payment.PaymentPayload
import domain.utils.letIf

class ReadyForAuthorization
(
    override val baseVersion: Int,
    override val newEvents: List<PaymentEvent>,
    override val newSideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: Int,
    val paymentAccount: PaymentAccount

): PaymentStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus =

        when (event)
        {
            is AuthorizationRequestedEvent -> apply(event, isNew)
            else -> this
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: AuthorizationRequestedEvent, isNew: Boolean): PaymentStatus
    {
        val newSideEffectEvents = newSideEffectEvents.toMutableList()
        val newEvents = if (isNew) newEvents + event else newEvents
        val newVersion = if (isNew) baseVersion else event.version

        newSideEffectEvents.addNewEvent(AuthorizationAttemptRequestedEvent, isNew)

        return when (event.authorizeResponse.status)
        {
            is AuthorizeStatus.Success ->
            {
                newSideEffectEvents.addNewEvent(PaymentAuthorizedEvent, isNew)

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
                newSideEffectEvents.addNewEvent(PaymentAuthenticationStartedEvent, isNew)

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

                return if (retryAttemps < MAX_RETRIES)
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
