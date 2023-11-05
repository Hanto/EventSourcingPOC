package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.Version
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.paymentevents.PaymentEvent
import domain.payment.paymentevents.PaymentRequestedEvent
import domain.payment.sideeffectevents.SideEffectEvent
import domain.payment.sideeffectevents.SideEffectEventList
import java.util.logging.Logger.getLogger

class ReadyForPaymentRequest : AbstractPayment(), Payment
{
    override val version: Version = Version.firstVersion()
    override val paymentEvents: List<PaymentEvent> = emptyList()
    override val sideEffectEvents: List<SideEffectEvent> = emptyList()
    override val attempt: Attempt = Attempt.firstNormalAttemp()

    private val log = getLogger(ReadyForPaymentRequest::class.java.name)

    override fun payload(): PaymentPayload = throw RuntimeException("Request not inputed yet")
    fun addPaymentPayload(paymentPayload: PaymentPayload): Payment
    {
        val event = PaymentRequestedEvent(
            paymentId = paymentPayload.id,
            version = version.nextEventVersion(paymentEvents),
            paymentPayload = paymentPayload)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is PaymentRequestedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: PaymentRequestedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return ReadyForRisk(
            version = newVersion,
            paymentEvents = newEvents,
            sideEffectEvents = newSideEffectEvents.list,
            attempt = attempt,
            payload = event.paymentPayload
        )
    }
}
