package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.routing.PaymentAccount
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload

data class RejectedByGateway
(
    override val baseVersion: Int,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: Int,
    val paymentAccount: PaymentAccount

) : AbstractPayment(), Payment
{
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
