package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.PaymentRequestedEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload

class ReadyForPaymentRequest : PaymentStatus
{
    override val paymentPayload: PaymentPayload? = null
    override val newSideEffectEvents: List<SideEffectEvent> = mutableListOf()

    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus =

        when (event)
        {
            is PaymentRequestedEvent -> apply(event, isNew)
            else -> this
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: PaymentRequestedEvent, isNew: Boolean): PaymentStatus =

        ReadyForRisk(
            paymentPayload = event.paymentPayload,
            newSideEffectEvents = newSideEffectEvents
        )
}
