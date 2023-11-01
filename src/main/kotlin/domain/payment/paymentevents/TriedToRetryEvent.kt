package domain.payment.paymentevents

import domain.payment.data.Version
import domain.payment.data.paymentpayload.PaymentId

data class TriedToRetryEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,

    ) : PaymentEvent
