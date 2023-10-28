package domain.authorize.events

import domain.authorize.steps.fraud.FraudAnalysisResult

data class RiskEvaluatedEvent
(
    override val version: Int,
    val fraudAnalysisResult: FraudAnalysisResult
): PaymentEvent
{
}
