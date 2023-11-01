package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.Version
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.SideEffectEvent

data class RejectedByRisk
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload

) : AbstractPayment(), Payment, Rejected, AuthorizeEnded
{
    override fun payload(): PaymentPayload = payload
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
