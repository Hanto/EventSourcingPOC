package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload

data class Failed
(
    override val baseVersion: Int,
    override val newEvents: List<PaymentEvent>,
    override val newSideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val reason: String,

): PaymentStatus
{
    override fun applyRecordedEvent(event: PaymentEvent): PaymentStatus = this
    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus = this
}
