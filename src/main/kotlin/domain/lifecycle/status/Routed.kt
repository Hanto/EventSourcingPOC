package domain.lifecycle.status

import domain.lifecycle.steps.fraud.RiskAssessmentOutcome
import domain.lifecycle.steps.routing.RoutingResult

sealed interface Routed: Payment
{
    val riskAssessmentOutcome: RiskAssessmentOutcome
    fun addRoutingResult(routingResult: RoutingResult): Payment
}
