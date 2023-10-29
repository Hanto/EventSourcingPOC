package domain.authorize.status

import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.routing.RoutingResult

interface ReadyForAnyRouting: PaymentStatus
{
    val riskAssessmentOutcome: RiskAssessmentOutcome
    fun addRoutingResult(routingResult: RoutingResult): PaymentStatus
}
