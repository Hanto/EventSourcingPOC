package domain.payment.state

import domain.payment.data.*
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.paymentevents.AuthenticateConfirmedEvent
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.*
import domain.services.gateway.ActionType
import domain.services.gateway.AuthenticateResponse
import java.util.logging.Logger

data class ReadyForAuthenticationAndAuthorizeConfirm
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount,
    val authenticateOutcome: AuthenticateOutcome.Performed,

): AbstractPayment(), Payment
{
    private val log = Logger.getLogger(ReadyForAuthenticationConfirm::class.java.name)

    override fun payload(): PaymentPayload = payload
    fun addAuthenticateConfirmResponse(authenticateResponse: AuthenticateResponse): Payment
    {
        val event = AuthenticateConfirmedEvent(
            paymentId = payload.id,
            version = version.nextEventVersion(paymentEvents),
            authenticateResponse = authenticateResponse)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is AuthenticateConfirmedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: AuthenticateConfirmedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return when (event.authenticateResponse)
        {
            is AuthenticateResponse.AuthenticateSuccess ->
            {
                newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)
                newSideEffectEvents.addIfNew(PaymentAuthenticationCompletedEvent, isNew)

                Failed(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = AuthenticateOutcome.Performed(event.authenticateResponse),
                    authorizeOutcome = null,
                    reason = "Response not valid for decoupled Authenticate flow"
                )
            }

            is AuthenticateResponse.AuthenticateAndAuthorizeSuccess ->
            {
                ReadyForCaptureVerification(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = AuthenticateOutcome.Performed(event.authenticateResponse),
                    authorizeOutcome = AuthorizeOutcome.Skipped
                )
            }

            is AuthenticateResponse.AuthenticateClientAction ->
            {
                newSideEffectEvents.addIfNew(getClientActionEvent(event.authenticateResponse), isNew)

                ReadyForAuthenticationAndAuthorizeClientAction(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = AuthenticateOutcome.Performed(event.authenticateResponse)
                )
            }

            is AuthenticateResponse.AuthenticateReject ->
            {
                newSideEffectEvents.addIfNew(AuthorizationAttemptRejectedEvent, isNew)
                newSideEffectEvents.addIfNew(PaymentAuthenticationCompletedEvent, isNew)

                return RejectedByAuthentication(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = AuthenticateOutcome.Performed(event.authenticateResponse),
                )
            }

            is AuthenticateResponse.AuthenticateFail ->
            {
                newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)
                newSideEffectEvents.addIfNew(PaymentAuthenticationCompletedEvent, isNew)

                Failed(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = AuthenticateOutcome.Performed(event.authenticateResponse),
                    authorizeOutcome = null,
                    reason = "exception on authorization"
                )
            }
        }
    }

    private fun getClientActionEvent(authorizeStatus: AuthenticateResponse.AuthenticateClientAction): SideEffectEvent =

        when(authorizeStatus.clientAction.type)
        {
            ActionType.FINGERPRINT -> BrowserFingerprintRequestedEvent
            ActionType.REDIRECT, ActionType.CHALLENGE -> UserApprovalRequestedEvent
        }
}
