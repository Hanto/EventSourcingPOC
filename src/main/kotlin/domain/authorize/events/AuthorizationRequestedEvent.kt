package domain.authorize.events

import domain.authorize.steps.gateway.AuthorizeResponse

data class AuthorizationRequestedEvent
(
    override val version: Int,
    val authorizeResponse: AuthorizeResponse

): PaymentEvent
{
}
