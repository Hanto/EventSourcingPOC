package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.PaymentRequestedEvent
import domain.events.SideEffectEvent
import domain.events.SideEffectEventList
import domain.payment.PaymentPayload
import domain.payment.Version
import java.util.logging.Logger.getLogger

class ReadyForPaymentRequest : AbstractPayment(), Payment
{
    override val baseVersion: Version = Version.firstVersion()
    override val paymentEvents: List<PaymentEvent> = emptyList()
    override val sideEffectEvents: List<SideEffectEvent> = emptyList()
    override val payload: PaymentPayload? = null
    private val log = getLogger(ReadyForPaymentRequest::class.java.name)

    fun addPaymentPayload(paymentPayload: PaymentPayload): Payment
    {
        val event = PaymentRequestedEvent(
            paymentId = paymentPayload.paymentId,
            version = baseVersion.nextEventVersion(paymentEvents),
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
        val newVersion = baseVersion.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return ReadyForRisk(
            baseVersion = newVersion,
            paymentEvents = newEvents,
            sideEffectEvents = newSideEffectEvents.list,
            payload = event.paymentPayload
        )
    }
}
