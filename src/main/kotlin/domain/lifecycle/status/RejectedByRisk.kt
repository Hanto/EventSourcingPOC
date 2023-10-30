package domain.lifecycle.status

import domain.events.SideEffectEvent
import domain.lifecycle.events.PaymentEvent
import domain.payment.PaymentPayload
import domain.payment.Version

data class RejectedByRisk
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val payload: PaymentPayload

) : AbstractPayment(), Payment, Rejected
{
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
