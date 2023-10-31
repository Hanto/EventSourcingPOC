package domain.payment.lifecycle.status

import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import domain.payment.Version
import domain.payment.lifecycle.events.PaymentEvent
import domain.services.fraud.RiskAssessmentOutcome

data class RejectedByRouting
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,

    ) : AbstractPayment(), Payment, Rejected
{
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
