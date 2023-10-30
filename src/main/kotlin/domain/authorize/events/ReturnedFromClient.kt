package domain.authorize.events

import domain.payment.Version

data class ReturnedFromClient
(
    override val version: Version,
    val confirmParameters: Map<String, Any>

): PaymentEvent
{
}
