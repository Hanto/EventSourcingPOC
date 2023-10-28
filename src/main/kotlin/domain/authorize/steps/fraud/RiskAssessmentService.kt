package domain.authorize.steps.fraud

import domain.payment.Payment

interface RiskAssessmentService
{
    fun assessRisk(payment: Payment): FraudAnalysisResult
}
