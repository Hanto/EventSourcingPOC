package domain.services.routing

import domain.payment.lifecycle.status.ReadyForRouting

interface RoutingService
{
    fun routeForPayment(payment: ReadyForRouting): RoutingResult
}
