package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.ReturnedFromClient
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.gateway.ActionType
import domain.authorize.steps.gateway.AuthorizeStatus
import domain.authorize.steps.gateway.ClientAction
import domain.authorize.steps.routing.PaymentAccount
import domain.events.BrowserFingerprintRequestedEvent
import domain.events.SideEffectEvent
import domain.events.UserApprovalRequestedEvent
import domain.payment.PaymentPayload
import java.util.logging.Logger

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
    private val log = Logger.getLogger(ReadyForClientActionResponse::class.java.name)

    fun addConfirmParameters(confirmParameters: Map<String, Any>): PaymentStatus
    {
        val event = ReturnedFromClient(
            version = baseVersion + newEvents.size + 1,
            confirmParameters = confirmParameters)

        return apply(event, isNew = true)
    }

    override fun applyRecordedEvent(event: PaymentEvent): PaymentStatus =

        apply(event, isNew = false)

    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus =

        when (event)
        {
            is ReturnedFromClient -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: ReturnedFromClient, isNew: Boolean): PaymentStatus
    {
        val newSideEffectEvents = newSideEffectEvents.toMutableList()
        val newEvents = if (isNew) newEvents + event else newEvents
        val newVersion = if (isNew) baseVersion else event.version

        return ReadyForConfirm(
            baseVersion = newVersion,
            newEvents = newEvents,
            newSideEffectEvents = newSideEffectEvents,
            paymentPayload = paymentPayload,
            riskAssessmentOutcome = riskAssessmentOutcome,
            retryAttemps = retryAttemps,
            paymentAccount = paymentAccount,
            confirmParameters = event.confirmParameters
        )
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
