package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.routing.PaymentAccount
import domain.payment.PaymentPayload
import domain.sideeffectevents.SideEffectEvent

data class Authorized
(
    override val newEvents: MutableList<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: Int,
    val paymentAccount: PaymentAccount

): AuthorizationStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): AuthorizationStatus {
        throw RuntimeException("payment in inconsistent status")
    }
}
