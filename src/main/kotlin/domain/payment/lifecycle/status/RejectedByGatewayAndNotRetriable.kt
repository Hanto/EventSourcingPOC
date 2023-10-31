package domain.payment.lifecycle.status

import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import domain.payment.RetryAttemp
import domain.payment.Version
import domain.payment.lifecycle.events.PaymentEvent
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.routing.PaymentAccount

data class RejectedByGatewayAndNotRetriable
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: RetryAttemp,
    val paymentAccount: PaymentAccount,

) : AbstractPayment(), Payment, Rejected, AuthorizeEnded
{
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
