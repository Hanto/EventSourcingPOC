package domain.payment.lifecycle.status

import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import domain.payment.RetryAttemp
import domain.payment.Version
import domain.payment.lifecycle.events.PaymentEvent
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.gateway.ThreeDSStatus
import domain.services.routing.PaymentAccount

data class Authorized
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: RetryAttemp,
    val paymentAccount: PaymentAccount,
    val threeDSStatus: ThreeDSStatus

): AbstractPayment(), Payment, AuthorizeEnded
{
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
