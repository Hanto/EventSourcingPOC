package domain.authorize.steps.routing

import domain.authorize.status.ReadyForAnyRouting

interface RoutingService
{
    fun routeForPayment(payment: ReadyForAnyRouting): RoutingResult
}
