package domain.payment.lifecycle.events

import domain.payment.Version
import domain.payment.payload.PaymentId
import domain.services.fraud.FraudAnalysisResult

data class RiskEvaluatedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val fraudAnalysisResult: FraudAnalysisResult

        ): PaymentEvent
{
}
