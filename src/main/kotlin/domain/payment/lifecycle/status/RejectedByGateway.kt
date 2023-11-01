package domain.payment.lifecycle.status

import domain.events.PaymentRejectedEvent
import domain.events.PaymentRetriedEvent
import domain.events.SideEffectEvent
import domain.events.SideEffectEventList
import domain.payment.Attempt
import domain.payment.Version
import domain.payment.lifecycle.events.PaymentEvent
import domain.payment.lifecycle.events.TriedToRetryEvent
import domain.payment.payload.PaymentPayload
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.gateway.PSPReference
import domain.services.gateway.ThreeDSStatus
import domain.services.routing.PaymentAccount
import java.util.logging.Logger

data class RejectedByGateway
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount,
    val threeDSStatus: ThreeDSStatus,
    val pspReference: PSPReference

) : AbstractPayment(), Payment, Rejected, AuthorizeEnded
{
    private val log = Logger.getLogger(ReadyForRoutingRetry::class.java.name)

    override fun payload(): PaymentPayload = payload
    fun prepareForRetry(): Payment
    {
        val event = TriedToRetryEvent(
            paymentId = payload.paymentId,
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
