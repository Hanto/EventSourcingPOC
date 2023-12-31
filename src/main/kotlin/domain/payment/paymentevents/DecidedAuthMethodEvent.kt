package domain.payment.paymentevents

import domain.payment.data.Version
import domain.payment.data.paymentpayload.PaymentId

data class DecidedAuthMethodEvent
(
    override val id: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val decuplingEnabled: Boolean,

): PaymentEvent
