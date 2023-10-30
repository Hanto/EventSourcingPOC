package domain.authorize.events

import domain.payment.PaymentId
import domain.payment.PaymentPayload
import domain.payment.Version

data class PaymentRequestedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val paymentPayload: PaymentPayload

): PaymentEvent
