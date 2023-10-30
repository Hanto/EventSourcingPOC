package domain.authorize.steps.routing

import domain.authorize.status.Routed

interface RoutingService
{
    fun routeForPayment(payment: Routed): RoutingResult
}
