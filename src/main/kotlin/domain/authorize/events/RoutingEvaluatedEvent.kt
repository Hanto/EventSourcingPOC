package domain.authorize.events

import domain.authorize.steps.routing.RoutingResult

data class RoutingEvaluatedEvent
(
    override val version: Int,
    val routingResult: RoutingResult

): PaymentEvent
{
}
