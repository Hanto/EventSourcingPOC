package domain.authorize.events

import domain.authorize.steps.gateway.AuthorizeResponse
import domain.payment.PaymentId
import domain.payment.Version

data class ConfirmationRequestedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val authorizeResponse: AuthorizeResponse

): PaymentEvent
