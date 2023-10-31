package domain.payment.lifecycle.events

import domain.payment.Version
import domain.payment.payload.PaymentId

data class TriedToRetryEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,

) : PaymentEvent
