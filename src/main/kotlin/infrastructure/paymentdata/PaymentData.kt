package infrastructure.paymentdata

import domain.services.fraud.RiskAssessmentOutcome

data class PaymentData
(
    val operations: List<AuthPaymentOperation>,
    val riskAssessmentOutcome: RiskAssessmentOutcome?
)
