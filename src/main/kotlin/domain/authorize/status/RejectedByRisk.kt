package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import domain.payment.Version

data class RejectedByRisk
(
    override val baseVersion: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload

) : AbstractPayment(), Payment
{
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
