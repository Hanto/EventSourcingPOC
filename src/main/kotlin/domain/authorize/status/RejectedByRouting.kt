package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload

data class RejectedByRouting
(
    override val newSideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,

    ) : RejectedStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus = this
}
