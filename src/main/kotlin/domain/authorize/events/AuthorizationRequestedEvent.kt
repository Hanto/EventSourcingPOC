package domain.authorize.events

import domain.authorize.steps.gateway.AuthorizeResponse
import domain.payment.Version

data class AuthorizationRequestedEvent
(
    override val version: Version,
    val authorizeResponse: AuthorizeResponse

): PaymentEvent
{
}
