package domain.authorize.events

import domain.authorize.steps.gateway.AuthorizeResponse

data class ConfirmationRequestedEvent
(
    override val version: Int,
    val authorizeResponse: AuthorizeResponse

): PaymentEvent
