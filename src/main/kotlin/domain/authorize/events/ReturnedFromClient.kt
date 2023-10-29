package domain.authorize.events

data class ReturnedFromClient
(
    override val version: Int,
    val confirmParameters: Map<String, Any>

): PaymentEvent
{
}
