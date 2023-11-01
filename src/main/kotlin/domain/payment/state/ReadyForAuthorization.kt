package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.data.paymentpayload.paymentmethod.KlarnaPayment
import domain.payment.paymentevents.AuthorizationRequestedEvent
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.*
import domain.services.gateway.ActionType
import domain.services.gateway.AuthorizeResponse
import java.util.logging.Logger

data class ReadyForAuthorization
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount

): AbstractPayment(), Payment, AuthorizeInProgress
{
    override fun payload(): PaymentPayload = payload
    private val log = Logger.getLogger(ReadyForAuthorization::class.java.name)

    fun addAuthorizeResponse(authorizeResponse: AuthorizeResponse): Payment
    {
        val event = AuthorizationRequestedEvent(
            paymentId = payload.paymentId,
            version = version.nextEventVersion(paymentEvents),
            authorizeResponse = authorizeResponse)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is AuthorizationRequestedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: AuthorizationRequestedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        newSideEffectEvents.addIfNew(AuthorizationAttemptRequestedEvent, isNew)

        return when (event.authorizeResponse)
        {
            is AuthorizeResponse.Success ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthorizedEvent, isNew)

                if (payload.paymentMethod is KlarnaPayment)
                    newSideEffectEvents.addIfNew(KlarnaOrderPlacedEvent, isNew)

                Authorized(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    threeDSStatus = event.authorizeResponse.threeDSStatus,
                    pspReference = event.authorizeResponse.pspReference,
                )
            }

            is AuthorizeResponse.ClientActionRequested ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthenticationStartedEvent, isNew)
                newSideEffectEvents.addIfNew(getClientActionEvent(event.authorizeResponse), isNew)

                ReadyForClientActionResponse(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    clientAction = event.authorizeResponse.clientAction,
                    threeDSStatus = event.authorizeResponse.threeDSStatus,
                    pspReference = event.authorizeResponse.pspReference,
                )
            }

            is AuthorizeResponse.Reject ->
            {
                newSideEffectEvents.addIfNew(AuthorizationAttemptRejectedEvent, isNew)

                return RejectedByGateway(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    threeDSStatus = event.authorizeResponse.threeDSStatus,
                    pspReference = event.authorizeResponse.pspReference,
                )
            }

            is AuthorizeResponse.Fail ->
            {
                newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

                Failed(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    threeDSStatus = event.authorizeResponse.threeDSStatus,
                    pspReference = event.authorizeResponse.pspReference,
                    reason = "exception on authorization"
                )
            }
        }
    }

    private fun getClientActionEvent(authorizeStatus: AuthorizeResponse.ClientActionRequested): SideEffectEvent =

        when(authorizeStatus.clientAction.type)
        {
            ActionType.FINGERPRINT -> BrowserFingerprintRequestedEvent
            ActionType.REDIRECT, ActionType.CHALLENGE -> UserApprovalRequestedEvent
        }
}
