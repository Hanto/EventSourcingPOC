package domain.lifecycle.events

import domain.payment.PaymentPayload
import domain.payment.Version
import domain.payment.payload.PaymentId

data class PaymentRequestedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val paymentPayload: PaymentPayload

): PaymentEvent
