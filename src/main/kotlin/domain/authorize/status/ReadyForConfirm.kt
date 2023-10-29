package domain.authorize.status

import domain.authorize.events.ConfirmationRequestedEvent
import domain.authorize.events.PaymentEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.gateway.ActionType
import domain.authorize.steps.gateway.AuthorizeResponse
import domain.authorize.steps.gateway.AuthorizeStatus
import domain.authorize.steps.routing.PaymentAccount
import domain.events.*
import domain.payment.PaymentPayload
import java.util.logging.Logger

class ReadyForConfirm
(
    override val baseVersion: Int,
    override val newEvents: List<PaymentEvent>,
    override val newSideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: Int,
    val paymentAccount: PaymentAccount,
    val confirmParameters: Map<String, Any>

): PaymentStatus
{
    companion object { private const val MAX_RETRIES = 1 }
    private val log = Logger.getLogger(ReadyForConfirm::class.java.name)

    fun addConfirmResponse(authorizeResponse: AuthorizeResponse): PaymentStatus
    {
        val event = ConfirmationRequestedEvent(
            version = baseVersion + newEvents.size + 1,
            authorizeResponse = authorizeResponse)

        return apply(event, isNew = true)
    }

    override fun applyRecordedEvent(event: PaymentEvent): PaymentStatus =

        apply(event, isNew = false)

    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus =

        when (event)
        {
            is ConfirmationRequestedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: ConfirmationRequestedEvent, isNew: Boolean): PaymentStatus
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
                newSideEffectEvents.addNewEvent(getClientActionEvent(event.authorizeResponse.status), isNew)

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

    private fun getClientActionEvent(authorizeStatus: AuthorizeStatus.ClientActionRequested): SideEffectEvent =

        when(authorizeStatus.clientAction.type)
        {
            ActionType.FINGERPRINT -> BrowserFingerprintRequestedEvent
            ActionType.REDIRECT, ActionType.CHALLENGE -> UserApprovalRequestedEvent
        }

    private fun MutableList<SideEffectEvent>.addNewEvent(event: SideEffectEvent, isNew: Boolean)
    {
        if (isNew)
            this.add(event)
    }
}
