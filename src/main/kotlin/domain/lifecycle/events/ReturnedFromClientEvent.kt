package domain.lifecycle.events

import domain.payment.Version
import domain.payment.payload.PaymentId

data class ReturnedFromClientEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val confirmParameters: Map<String, Any>

): PaymentEvent
{
}
