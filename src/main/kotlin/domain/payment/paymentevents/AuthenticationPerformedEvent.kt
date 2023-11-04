package domain.payment.paymentevents

import domain.payment.data.Version
import domain.payment.data.paymentpayload.PaymentId
import domain.services.gateway.AuthenticateResponse

data class AuthenticationPerformedEvent
(
    override val id: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val authenticateResponse: AuthenticateResponse

): PaymentEvent
