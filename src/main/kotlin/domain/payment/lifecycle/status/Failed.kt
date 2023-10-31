package domain.payment.lifecycle.status

import domain.events.SideEffectEvent
import domain.payment.Attempt
import domain.payment.Version
import domain.payment.lifecycle.events.PaymentEvent
import domain.payment.payload.PaymentPayload
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.gateway.ThreeDSStatus
import domain.services.routing.PaymentAccount

data class Failed
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome? = null,
    val paymentAccount: PaymentAccount? = null,
    val threeDSStatus: ThreeDSStatus? = null,
    val reason: String,

    ): AbstractPayment(), Payment, AuthorizeEnded
{
    override fun payload(): PaymentPayload = payload
    override fun apply(event: PaymentEvent, isNew: Boolean): Payment = this
}
