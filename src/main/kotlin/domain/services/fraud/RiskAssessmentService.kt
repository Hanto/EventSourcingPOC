package domain.services.fraud

import domain.payment.state.ReadyForRisk

interface RiskAssessmentService
{
    fun assessRisk(payment: ReadyForRisk): FraudAnalysisResult
}
