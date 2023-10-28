package domain.authorize.steps.routing

import domain.payment.Payment

interface RoutingService
{
    fun routeForPayment(payment: Payment): RoutingResult
}
