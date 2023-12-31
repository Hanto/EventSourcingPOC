package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.data.paymentpayload.paymentmethod.KlarnaPayment
import domain.payment.paymentevents.AuthenticateContinuedEvent
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.*
import domain.services.gateway.ActionType
import domain.services.gateway.AuthenticateOutcome
import domain.services.gateway.AuthenticateResponse
import domain.services.gateway.AuthorizeOutcome
import java.util.logging.Logger

data class ReadyToContinueAuthentication
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount,
    val authenticateOutcome: AuthenticateOutcome,

): AbstractPayment(), Payment
{
    private val log = Logger.getLogger(ReadyToContinueAuthentication::class.java.name)

    override fun payload(): PaymentPayload = payload
    fun addAuthenticateConfirmResponse(authenticateResponse: AuthenticateResponse): Payment
    {
        val event = AuthenticateContinuedEvent(
            paymentId = payload.id,
            version = version.nextEventVersion(paymentEvents),
            authenticateResponse = authenticateResponse)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is AuthenticateContinuedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: AuthenticateContinuedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return when (event.authenticateResponse)
        {
            is AuthenticateResponse.AuthenticateSuccess ->
            {
                ReadyToVerifyAuthentication(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = event.authenticateResponse,
                )
            }
            // OPTIONAL FLOW: (TBD)

            is AuthenticateResponse.AuthenticateAndAuthorizeSuccess ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthorizedEvent, isNew)

                if (payload.paymentMethod is KlarnaPayment)
                    newSideEffectEvents.addIfNew(KlarnaOrderPlacedEvent, isNew)

                ReadyToEndAuthorization(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = event.authenticateResponse,
                    authorizeOutcome = AuthorizeOutcome.Skipped
                )
            }

            is AuthenticateResponse.AuthenticateClientAction ->
            {
                newSideEffectEvents.addIfNew(getClientActionEvent(event.authenticateResponse), isNew)

                ReadyToReturnFromAuthentication(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = event.authenticateResponse,
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
                    authenticateOutcome = event.authenticateResponse,
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
                    authenticateOutcome = event.authenticateResponse,
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
