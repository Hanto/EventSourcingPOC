package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.routing.PaymentAccount
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload

class RejectedByGateway
(
    override val newSideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: Int,
    val paymentAccount: PaymentAccount

) : RejectedStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus = this
}
