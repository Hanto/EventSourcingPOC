package domain.lifecycle.events

import domain.lifecycle.steps.gateway.AuthorizeResponse
import domain.payment.Version
import domain.payment.payload.PaymentId

data class AuthorizationRequestedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val authorizeResponse: AuthorizeResponse

): PaymentEvent
{
}
