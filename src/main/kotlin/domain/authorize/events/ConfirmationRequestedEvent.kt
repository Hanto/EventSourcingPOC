package domain.authorize.events

import domain.authorize.steps.gateway.AuthorizeResponse
import domain.payment.Version

data class ConfirmationRequestedEvent
(
    override val version: Version,
    val authorizeResponse: AuthorizeResponse

): PaymentEvent
