package domain.authorize.events

import domain.authorize.steps.fraud.FraudAnalysisResult
import domain.payment.Version
import domain.payment.payload.PaymentId

data class RiskEvaluatedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val fraudAnalysisResult: FraudAnalysisResult

        ): PaymentEvent
{
}
