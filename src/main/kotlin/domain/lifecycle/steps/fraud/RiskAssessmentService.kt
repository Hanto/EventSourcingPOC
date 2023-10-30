package domain.lifecycle.steps.fraud

import domain.lifecycle.status.ReadyForRisk

interface RiskAssessmentService
{
    fun assessRisk(payment: ReadyForRisk): FraudAnalysisResult
}
