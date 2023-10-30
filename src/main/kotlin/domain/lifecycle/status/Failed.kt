package domain.lifecycle.status

import domain.events.SideEffectEvent
import domain.lifecycle.events.PaymentEvent
import domain.payment.PaymentPayload
import domain.payment.Version

data class Failed
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val payload: PaymentPayload,
    val reason: String,

    ): AbstractPayment(), Payment
{
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
