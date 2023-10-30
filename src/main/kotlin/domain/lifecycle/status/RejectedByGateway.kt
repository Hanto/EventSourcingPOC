package domain.lifecycle.status

import domain.events.SideEffectEvent
import domain.lifecycle.events.PaymentEvent
import domain.lifecycle.steps.fraud.RiskAssessmentOutcome
import domain.lifecycle.steps.routing.PaymentAccount
import domain.payment.PaymentPayload
import domain.payment.RetryAttemp
import domain.payment.Version

data class RejectedByGateway
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: RetryAttemp,
    val paymentAccount: PaymentAccount

) : AbstractPayment(), Payment, Rejected
{
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
