package domain.payment.lifecycle.events

import domain.payment.Version
import domain.payment.payload.PaymentId
import domain.payment.payload.PaymentPayload

data class PaymentRequestedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val paymentPayload: PaymentPayload

): PaymentEvent
