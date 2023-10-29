package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload

data class Failed
(
    override val newSideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val reason: String,

    ): PaymentStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus = this
}
