package infrastructure.repositories.paymentrepositoryold

import domain.payment.data.RiskAssessmentOutcome
import java.util.*

data class PaymentData
(
    val id: UUID,
    val authorizationReference: String,
    val riskAssessmentOutcome: RiskAssessmentOutcome?,
    val operations: List<AuthPaymentOperation>
)
