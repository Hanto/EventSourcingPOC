package domain.services.fraud

sealed interface FraudAnalysisResult
{
    data class Approved(
        val riskAssessmentOutcome: RiskAssessmentOutcome,
    ): FraudAnalysisResult

    data object Denied: FraudAnalysisResult
}
