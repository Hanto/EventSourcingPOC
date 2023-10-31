package infrastructure.paymentdata

import domain.services.fraud.RiskAssessmentOutcome
import java.util.*

data class PaymentData
(
    val id: UUID,
    val authorizationReference: String,
    val operations: List<AuthPaymentOperation>,
    val riskAssessmentOutcome: RiskAssessmentOutcome?
)
