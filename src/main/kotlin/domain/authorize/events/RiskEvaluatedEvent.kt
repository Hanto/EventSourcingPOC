package domain.authorize.events

import domain.authorize.steps.fraud.FraudAnalysisResult
import domain.payment.Version

data class RiskEvaluatedEvent
(
    override val version: Version,
    val fraudAnalysisResult: FraudAnalysisResult
): PaymentEvent
{
}
