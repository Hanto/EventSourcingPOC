package domain.authorize.events

import domain.authorize.steps.gateway.AuthorizeResponse

data class ConfirmedEvent
(
    override val version: Int,
    val authorizeResponse: AuthorizeResponse,
    val confirmParameters: Map<String, Any>

): PaymentEvent
{
}
