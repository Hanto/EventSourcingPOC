package domain.authorize.events

import domain.payment.PaymentId
import domain.payment.Version

data class ReturnedFromClientEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val confirmParameters: Map<String, Any>

): PaymentEvent
{
}
