package domain.services.fraud

import domain.payment.data.RiskAssessmentOutcome

sealed interface FraudAnalysisResult
{
    data class Approved(
        val riskAssessmentOutcome: RiskAssessmentOutcome,
    ): FraudAnalysisResult

    data object Denied: FraudAnalysisResult
}
