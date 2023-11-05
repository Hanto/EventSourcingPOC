package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.SideEffectEvent
import domain.services.gateway.AuthenticateResponse
import domain.services.gateway.AuthorizeResponse

data class Captured
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount,
    val authenticateResponse: AuthenticateResponse,
    val authorizeResponse: AuthorizeResponse.AuthorizeSuccess,

): AbstractPayment(), Payment
{
    override fun payload(): PaymentPayload = payload
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
