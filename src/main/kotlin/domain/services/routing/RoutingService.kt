package domain.services.routing

import domain.payment.state.ReadyForRouting

interface RoutingService
{
    fun routeForPayment(payment: ReadyForRouting): RoutingResult
}
