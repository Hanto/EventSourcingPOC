package domain.payment.lifecycle.status

import domain.services.fraud.RiskAssessmentOutcome
import domain.services.routing.RoutingResult

sealed interface ReadyForRouting: Payment, AuthorizeInProgress
{
    val riskAssessmentOutcome: RiskAssessmentOutcome
    fun addRoutingResult(routingResult: RoutingResult): Payment
}
