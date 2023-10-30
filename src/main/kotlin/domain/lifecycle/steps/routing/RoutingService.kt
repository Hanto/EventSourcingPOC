package domain.lifecycle.steps.routing

import domain.lifecycle.status.Routed

interface RoutingService
{
    fun routeForPayment(payment: Routed): RoutingResult
}
