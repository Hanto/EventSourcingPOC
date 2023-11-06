package domain.payment.state

import domain.payment.data.*
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.data.paymentpayload.paymentmethod.KlarnaPayment
import domain.payment.paymentevents.AuthenticationAndAuthorizationPerformedEvent
import domain.payment.paymentevents.AuthenticationPerformedEvent
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.*
import domain.services.gateway.ActionType
import domain.services.gateway.AuthenticateResponse
import java.util.logging.Logger

data class ReadyForAuthentication
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount

): AbstractPayment(), Payment
{
    private val log = Logger.getLogger(ReadyForAuthentication::class.java.name)

    override fun payload(): PaymentPayload = payload
    fun addAuthenticationResponse(authenticateResponse: AuthenticateResponse): Payment
    {
        val event = AuthenticationPerformedEvent(
            paymentId = payload.id,
            version = version.nextEventVersion(paymentEvents),
            authenticateResponse = authenticateResponse)

        return apply(event, isNew = true)
    }

    fun addAuthenticationAndAuthorizationResponse(authenticateResponse: AuthenticateResponse): Payment
    {
        val event = AuthenticationAndAuthorizationPerformedEvent(
            paymentId = payload.id,
            version = version.nextEventVersion(paymentEvents),
            authenticateResponse = authenticateResponse)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is AuthenticationPerformedEvent -> apply(event, isNew)
            is AuthenticationAndAuthorizationPerformedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT: (AUTHENTICATION)
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: AuthenticationPerformedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        newSideEffectEvents.addIfNew(AuthorizationAttemptRequestedEvent, isNew)

        return when (event.authenticateResponse)
        {
            is AuthenticateResponse.AuthenticateSuccess ->
            {
                ReadyForAuthorization(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateResponse = AuthenticateOutcome.Performed(event.authenticateResponse)
                )
            }
            // OPTIONAL FLOW: (TBD)

            is AuthenticateResponse.AuthenticateAndAuthorizeSuccess ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthorizedEvent, isNew)

                if (payload.paymentMethod is KlarnaPayment)
                    newSideEffectEvents.addIfNew(KlarnaOrderPlacedEvent, isNew)

                ReadyForCaptureVerification(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateResponse = AuthenticateOutcome.Performed(event.authenticateResponse),
                    authorizeResponse = AuthorizeOutcome.Skipped
                )
            }

            is AuthenticateResponse.AuthenticateClientAction ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthenticationStartedEvent, isNew)
                newSideEffectEvents.addIfNew(getClientActionEvent(event.authenticateResponse), isNew)

                ReadyForAuthenticationClientAction(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateResponse = AuthenticateOutcome.Performed(event.authenticateResponse)
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
                    authenticateResponse = AuthenticateOutcome.Performed(event.authenticateResponse)
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
                    authenticateResponse = AuthenticateOutcome.Performed(event.authenticateResponse),
                    authorizeResponse = null,
                    reason = "Authentication failed"
                )
            }
        }
    }

    // APPLY EVENT: (AUTHENTICATION AND AUTHORIZATION)
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: AuthenticationAndAuthorizationPerformedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        newSideEffectEvents.addIfNew(AuthorizationAttemptRequestedEvent, isNew)

        return when (event.authenticateResponse)
        {
            // INVALID RESPONSE: (TBD)

            is AuthenticateResponse.AuthenticateSuccess ->
            {
                Failed(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateResponse = AuthenticateOutcome.Performed(event.authenticateResponse),
                    authorizeResponse = null,
                    reason = "Response not valid for not-decoupled Authenticate flow"
                )
            }

            is AuthenticateResponse.AuthenticateAndAuthorizeSuccess ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthorizedEvent, isNew)

                if (payload.paymentMethod is KlarnaPayment)
                    newSideEffectEvents.addIfNew(KlarnaOrderPlacedEvent, isNew)

                ReadyForCaptureVerification(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateResponse = AuthenticateOutcome.Performed(event.authenticateResponse),
                    authorizeResponse = AuthorizeOutcome.Skipped
                )
            }

            is AuthenticateResponse.AuthenticateClientAction ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthenticationStartedEvent, isNew)
                newSideEffectEvents.addIfNew(getClientActionEvent(event.authenticateResponse), isNew)

                ReadyForAuthenticationAndAuthorizeClientAction(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateResponse = AuthenticateOutcome.Performed(event.authenticateResponse)
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
                    authenticateResponse = AuthenticateOutcome.Performed(event.authenticateResponse)
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
                    authenticateResponse = AuthenticateOutcome.Performed(event.authenticateResponse),
                    authorizeResponse = null,
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
