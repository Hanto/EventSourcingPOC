package domain.payment.paymentevents

import domain.payment.data.Version
import domain.payment.data.paymentpayload.PaymentId
import domain.payment.data.paymentpayload.PaymentPayload

data class PaymentRequestedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val paymentPayload: PaymentPayload

): PaymentEvent
