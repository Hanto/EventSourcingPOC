package domain.authorize.steps.fraud

import domain.authorize.status.ReadyForRisk

interface RiskAssessmentService
{
    fun assessRisk(payment: ReadyForRisk): FraudAnalysisResult
}
