package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.routing.PaymentAccount
import domain.payment.PaymentPayload
import domain.sideeffectevents.SideEffectEvent

class RejectedByGateway
(
    override val newSideEffectEvents: MutableList<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: Int,
    val paymentAccount: PaymentAccount

) : RejectedStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): AuthorizationStatus = this
}
