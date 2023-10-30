package domain.authorize.events

import domain.authorize.steps.routing.RoutingResult
import domain.payment.Version

data class RoutingEvaluatedEvent
(
    override val version: Version,
    val routingResult: RoutingResult

): PaymentEvent
{
}
