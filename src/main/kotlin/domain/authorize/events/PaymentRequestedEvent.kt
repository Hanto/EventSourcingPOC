package domain.authorize.events

import domain.payment.PaymentPayload
import domain.payment.Version

data class PaymentRequestedEvent
(
    override val version: Version,
    val paymentPayload: PaymentPayload

): PaymentEvent
