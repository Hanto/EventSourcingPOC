package domain.payment.state

import domain.payment.data.RiskAssessmentOutcome
import domain.services.routing.RoutingResult

sealed interface ReadyForRouting: Payment
{
    val riskAssessmentOutcome: RiskAssessmentOutcome
    fun addRoutingResult(routingResult: RoutingResult): Payment
}
