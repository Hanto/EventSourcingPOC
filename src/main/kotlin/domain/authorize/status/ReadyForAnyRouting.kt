package domain.authorize.status

import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.routing.RoutingResult

sealed interface ReadyForAnyRouting: Payment
{
    val riskAssessmentOutcome: RiskAssessmentOutcome
    fun addRoutingResult(routingResult: RoutingResult): Payment
}
