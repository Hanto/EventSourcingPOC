package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.PaymentRequestedEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import java.util.logging.Logger.getLogger

class ReadyForPaymentRequest : PaymentStatus
{
    override val baseVersion: Int = 0
    override val newEvents: List<PaymentEvent> = emptyList()
    override val newSideEffectEvents: List<SideEffectEvent> = emptyList()
    override val paymentPayload: PaymentPayload? = null
    private val log = getLogger(ReadyForPaymentRequest::class.java.name)

    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus =

        when (event)
        {
            is PaymentRequestedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: PaymentRequestedEvent, isNew: Boolean): PaymentStatus
    {
        val newSideEffectEvents = newSideEffectEvents.toMutableList()
        val newEvents = if (isNew) newEvents + event else newEvents
        val newVersion = if (isNew) baseVersion else event.version

        return ReadyForRisk(
            baseVersion = newVersion,
            newEvents = newEvents,
            paymentPayload = event.paymentPayload,
            newSideEffectEvents = newSideEffectEvents
        )
    }
}
