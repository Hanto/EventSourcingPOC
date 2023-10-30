package domain.payment.lifecycle.status

import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import domain.payment.Version
import domain.payment.lifecycle.events.PaymentEvent

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
