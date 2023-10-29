package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.PaymentRequestedEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import java.util.logging.Logger.getLogger

class ReadyForPaymentRequest : AbstractPayment(), Payment
{
    override val baseVersion: Int = 0
    override val paymentEvents: List<PaymentEvent> = emptyList()
    override val sideEffectEvents: List<SideEffectEvent> = emptyList()
    override val paymentPayload: PaymentPayload? = null
    private val log = getLogger(ReadyForPaymentRequest::class.java.name)

    fun addPaymentPayload(paymentPayload: PaymentPayload): Payment
    {
        val event = PaymentRequestedEvent(
            version = nextVersion(),
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
        val newEvents = addEventIfNew(event, isNew)
        val newVersion = upgradeVersionIfReplay(event, isNew)
        val newSideEffectEvents = toMutableSideEffectEvents()

        return ReadyForRisk(
            baseVersion = newVersion,
            paymentEvents = newEvents,
            sideEffectEvents = newSideEffectEvents,
            paymentPayload = event.paymentPayload
        )
    }
}
