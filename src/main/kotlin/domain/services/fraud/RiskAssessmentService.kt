package domain.services.fraud

import domain.payment.lifecycle.status.ReadyForRisk

interface RiskAssessmentService
{
    fun assessRisk(payment: ReadyForRisk): FraudAnalysisResult
}
