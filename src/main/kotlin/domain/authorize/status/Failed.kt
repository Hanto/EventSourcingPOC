package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload

data class Failed
(
    override val baseVersion: Int,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val reason: String,

    ): AbstractPayment(), Payment
{
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
