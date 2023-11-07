package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.SideEffectEvent
import domain.services.gateway.AuthenticateOutcome
import domain.services.gateway.AuthorizeOutcome

data class Failed
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome?,
    val paymentAccount: PaymentAccount?,
    val authenticateOutcome: AuthenticateOutcome?,
    val authorizeOutcome: AuthorizeOutcome?,
    val reason: String,

    ): AbstractPayment(), Payment
{
    override fun payload(): PaymentPayload = payload
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
