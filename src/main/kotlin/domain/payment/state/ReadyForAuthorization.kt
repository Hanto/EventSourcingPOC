package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.data.paymentpayload.paymentmethod.KlarnaPayment
import domain.payment.paymentevents.AuthorizationPerformedEvent
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.*
import domain.services.gateway.AuthenticateOutcome
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
    val paymentAccount: PaymentAccount,
    val authenticateOutcome: AuthenticateOutcome,

): AbstractPayment(), Payment
{
    private val log = Logger.getLogger(ReadyForAuthentication::class.java.name)

    override fun payload(): PaymentPayload = payload
    fun addAuthorizeResponse(authorizeResponse: AuthorizeResponse): Payment
    {
        val event = AuthorizationPerformedEvent(
            paymentId = payload.id,
            version = version.nextEventVersion(paymentEvents),
            authorizeResponse = authorizeResponse)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is AuthorizationPerformedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: AuthorizationPerformedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return when (event.authorizeResponse)
        {
            is AuthorizeResponse.AuthorizeSuccess ->
            {
                newSideEffectEvents.addIfNew(PaymentAuthorizedEvent, isNew)

                if (payload.paymentMethod is KlarnaPayment)
                    newSideEffectEvents.addIfNew(KlarnaOrderPlacedEvent, isNew)

                ReadyForCaptureVerification(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = authenticateOutcome,
                    authorizeOutcome = event.authorizeResponse,
                )
            }

            is AuthorizeResponse.AuthorizeReject ->
            {
                newSideEffectEvents.addIfNew(AuthorizationAttemptRejectedEvent, isNew)

                return RejectedByAuthorization(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = authenticateOutcome,
                    authorizeOutcome = event.authorizeResponse,
                )
            }

            is AuthorizeResponse.AuthorizeFail ->
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
                    authenticateOutcome = authenticateOutcome,
                    authorizeOutcome = event.authorizeResponse,
                    reason = "exception on authorization"
                )
            }
        }
    }
}
