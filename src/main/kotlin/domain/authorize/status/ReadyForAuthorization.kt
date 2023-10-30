package domain.authorize.status

import domain.authorize.events.AuthorizationRequestedEvent
import domain.authorize.events.PaymentEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.gateway.ActionType
import domain.authorize.steps.gateway.AuthorizeResponse
import domain.authorize.steps.gateway.AuthorizeStatus
import domain.authorize.steps.routing.PaymentAccount
import domain.events.*
import domain.payment.PaymentPayload
import domain.payment.RetryAttemp
import domain.payment.Version
import java.util.logging.Logger

data class ReadyForAuthorization
(
    override val baseVersion: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: RetryAttemp,
    val paymentAccount: PaymentAccount

): AbstractPayment(), Payment
{
    companion object { private const val MAX_RETRIES = 1 }
    private val log = Logger.getLogger(ReadyForAuthorization::class.java.name)

    fun addAuthorizeResponse(authorizeResponse: AuthorizeResponse): Payment
    {
        val event = AuthorizationRequestedEvent(
            version = baseVersion.nextEventVersion(paymentEvents),
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
        val newVersion = baseVersion.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        newSideEffectEvents.addIfNew(AuthorizationAttemptRequestedEvent, isNew)

        return when (event.authorizeResponse.status)
        {
            is AuthorizeStatus.Success ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthorizedEvent, isNew)

                Authorized(
                    baseVersion = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    paymentPayload = paymentPayload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemps = retryAttemps,
                    paymentAccount = paymentAccount
                )
            }

            is AuthorizeStatus.ClientActionRequested ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthenticationStartedEvent, isNew)
                newSideEffectEvents.addIfNew(getClientActionEvent(event.authorizeResponse.status), isNew)

                ReadyForClientActionResponse(
                    baseVersion = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    paymentPayload = paymentPayload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemps = retryAttemps,
                    paymentAccount = paymentAccount,
                    clientAction = event.authorizeResponse.status.clientAction
                )
            }

            is AuthorizeStatus.Reject ->
            {
                newSideEffectEvents.addIfNew(AuthorizationAttemptRejectedEvent, isNew)

                return if (retryAttemps.value < MAX_RETRIES)
                {
                    newSideEffectEvents.addIfNew(PaymentRetriedEvent, isNew)

                    ReadyForRoutingRetry(
                        baseVersion = newVersion,
                        paymentEvents = newEvents,
                        sideEffectEvents = newSideEffectEvents.list,
                        paymentPayload = paymentPayload,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        retryAttemps = retryAttemps.next(),
                        paymentAccount = paymentAccount
                    )
                }
                else
                {
                    newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

                    RejectedByGateway(
                        baseVersion = newVersion,
                        paymentEvents = newEvents,
                        sideEffectEvents = newSideEffectEvents.list,
                        paymentPayload = paymentPayload,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        retryAttemps = retryAttemps,
                        paymentAccount = paymentAccount
                    )
                }
            }

            is AuthorizeStatus.Fail ->
            {
                newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

                Failed(
                    baseVersion = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    paymentPayload = paymentPayload,
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
}
