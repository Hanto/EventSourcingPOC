package domain.payment.lifecycle.events

import domain.payment.Version
import domain.payment.payload.PaymentId
import domain.services.gateway.AuthorizeResponse

data class ConfirmationRequestedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val authorizeResponse: AuthorizeResponse

): PaymentEvent
