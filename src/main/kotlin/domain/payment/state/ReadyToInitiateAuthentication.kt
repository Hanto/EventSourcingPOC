package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.paymentevents.AuthenticateStartedEvent
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.*
import domain.services.gateway.ActionType
import domain.services.gateway.AuthenticateResponse
import domain.services.gateway.AuthorizationGateway
import java.util.logging.Logger

data class ReadyToInitiateAuthentication
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount

): AbstractPayment(), Payment, ReadyForAuth
{
    private val log = Logger.getLogger(ReadyToInitiateAuthentication::class.java.name)

    override fun payload(): PaymentPayload = payload

    override fun addAuthenticationResponse(authorizeService: AuthorizationGateway): Payment
    {
        val authenticateResponse = authorizeService.authenticate(this)

        val event = AuthenticateStartedEvent(
            paymentId = payload.id,
            version = version.nextEventVersion(paymentEvents),
            authenticateResponse = authenticateResponse)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is AuthenticateStartedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT: (AUTHENTICATION)
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: AuthenticateStartedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        newSideEffectEvents.addIfNew(AuthorizationAttemptRequestedEvent, isNew)

        return when (event.authenticateResponse)
        {
            is AuthenticateResponse.AuthenticateSuccess ->
            {
                ReadyToVerifyAuthentication(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = event.authenticateResponse
                )
            }
            // OPTIONAL FLOW: (TBD)

            is AuthenticateResponse.AuthenticateAndAuthorizeSuccess ->
            {
                newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

                Failed(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = event.authenticateResponse,
                    authorizeOutcome = null,
                    reason = "Response not valid for decoupled Authenticate flow"
                )
            }

            is AuthenticateResponse.AuthenticateClientAction ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthenticationStartedEvent, isNew)
                newSideEffectEvents.addIfNew(getClientActionEvent(event.authenticateResponse), isNew)

                ReadyToReturnFromAuthentication(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = event.authenticateResponse
                )
            }

            is AuthenticateResponse.AuthenticateReject ->
            {
                newSideEffectEvents.addIfNew(AuthorizationAttemptRejectedEvent, isNew)

                RejectedByAuthentication(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = event.authenticateResponse
                )
            }

            is AuthenticateResponse.AuthenticateFail ->
            {
                newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

                Failed(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = event.authenticateResponse,
                    authorizeOutcome = null,
                    reason = "Authentication failed"
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
