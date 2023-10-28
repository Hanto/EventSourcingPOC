package domain.authorize.events

import domain.payment.PaymentPayload

data class PaymentRequestedEvent
(
    override val version: Int,
    val paymentPayload: PaymentPayload

): PaymentEvent
