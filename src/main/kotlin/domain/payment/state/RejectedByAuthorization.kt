package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.paymentevents.PaymentEvent
import domain.payment.paymentevents.TriedToRetryEvent
import domain.payment.sideeffectevents.PaymentRejectedEvent
import domain.payment.sideeffectevents.PaymentRetriedEvent
import domain.payment.sideeffectevents.SideEffectEvent
import domain.payment.sideeffectevents.SideEffectEventList
import domain.services.gateway.AuthenticateResponse
import domain.services.gateway.AuthorizeResponse
import java.util.logging.Logger

data class RejectedByAuthorization
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount,
    val authenticateResponse: AuthenticateResponse.AuthenticateSuccess,
    val authorizeResponse: AuthorizeResponse.AuthorizeReject

): AbstractPayment(), Payment, Rejected, RejectedByGateway
{
    private val log = Logger.getLogger(RejectedByAuthorization::class.java.name)

    override fun payload(): PaymentPayload = payload
    override fun prepareForRetry(): Payment
    {
        val event = TriedToRetryEvent(
            paymentId = payload.id,
            version = version.nextEventVersion(paymentEvents),
        )

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is TriedToRetryEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: TriedToRetryEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return if (attempt.canRetry())
        {
            newSideEffectEvents.addIfNew(PaymentRetriedEvent, isNew)

            ReadyForRoutingRetry(
                version = newVersion,
                paymentEvents = newEvents,
                sideEffectEvents = newSideEffectEvents.list,
                attempt = attempt.next(),
                payload = payload,
                riskAssessmentOutcome = riskAssessmentOutcome,
                paymentAccount = paymentAccount,
            )
        }
        else
        {
            newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

            RejectedByGatewayAndNotRetriable(
                version = newVersion,
                paymentEvents = newEvents,
                sideEffectEvents = newSideEffectEvents.list,
                attempt = attempt,
                payload = payload,
                riskAssessmentOutcome = riskAssessmentOutcome,
                paymentAccount = paymentAccount
            )
        }
    }
}
