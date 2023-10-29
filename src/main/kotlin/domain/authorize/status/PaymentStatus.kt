package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload

sealed interface PaymentStatus
{
    val newSideEffectEvents: MutableList<SideEffectEvent>
    val paymentPayload: PaymentPayload?
    fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus
}
