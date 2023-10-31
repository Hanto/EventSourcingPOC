package domain.payment.lifecycle.status

import domain.events.*
import domain.payment.PaymentPayload
import domain.payment.RetryAttemp
import domain.payment.Version
import domain.payment.lifecycle.events.ConfirmationRequestedEvent
import domain.payment.lifecycle.events.PaymentEvent
import domain.payment.payload.paymentmethod.KlarnaPayment
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.gateway.ActionType
import domain.services.gateway.AuthorizeResponse
import domain.services.routing.PaymentAccount
import java.util.logging.Logger

data class ReadyForConfirm
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: RetryAttemp,
    val paymentAccount: PaymentAccount,
    val confirmParameters: Map<String, Any>

): AbstractPayment(), Payment, AuthorizeInProgress
{
    private val log = Logger.getLogger(ReadyForConfirm::class.java.name)

    fun addConfirmResponse(authorizeResponse: AuthorizeResponse): Payment
    {
        val event = ConfirmationRequestedEvent(
            paymentId = payload.paymentId,
            version = version.nextEventVersion(paymentEvents),
            authorizeResponse = authorizeResponse)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is ConfirmationRequestedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: ConfirmationRequestedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return when (event.authorizeResponse)
        {
            is AuthorizeResponse.Success ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthorizedEvent, isNew)
                newSideEffectEvents.addIfNew(PaymentAuthenticationCompletedEvent, isNew)

                if (payload.paymentMethod is KlarnaPayment)
                    newSideEffectEvents.addIfNew(KlarnaOrderPlacedEvent, isNew)

                Authorized(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemps = retryAttemps,
                    paymentAccount = paymentAccount,
                    threeDSStatus = event.authorizeResponse.threeDSStatus
                )
            }

            is AuthorizeResponse.ClientActionRequested ->
            {
                newSideEffectEvents.addIfNew(getClientActionEvent(event.authorizeResponse), isNew)

                ReadyForClientActionResponse(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemps = retryAttemps,
                    paymentAccount = paymentAccount,
                    clientAction = event.authorizeResponse.clientAction,
                    threeDSStatus = event.authorizeResponse.threeDSStatus
                )
            }

            is AuthorizeResponse.Reject ->
            {
                newSideEffectEvents.addIfNew(AuthorizationAttemptRejectedEvent, isNew)
                newSideEffectEvents.addIfNew(PaymentAuthenticationCompletedEvent, isNew)

                if (retryAttemps.canRetry())
                {
                    newSideEffectEvents.addIfNew(PaymentRetriedEvent, isNew)

                    ReadyForRoutingRetry(
                        version = newVersion,
                        paymentEvents = newEvents,
                        sideEffectEvents = newSideEffectEvents.list,
                        payload = payload,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        retryAttemps = retryAttemps.next(),
                        paymentAccount = paymentAccount,
                    )
                }
                else
                {
                    newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

                    RejectedByGateway(
                        version = newVersion,
                        paymentEvents = newEvents,
                        sideEffectEvents = newSideEffectEvents.list,
                        payload = payload,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        retryAttemps = retryAttemps,
                        paymentAccount = paymentAccount,
                        threeDSStatus = event.authorizeResponse.threeDSStatus
                    )
                }
            }

            is AuthorizeResponse.Fail ->
            {
                newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)
                newSideEffectEvents.addIfNew(PaymentAuthenticationCompletedEvent, isNew)

                Failed(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    payload = payload,
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