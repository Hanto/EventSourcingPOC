package domain.authorize.events

import domain.authorize.steps.routing.RoutingResult
import domain.payment.PaymentId
import domain.payment.Version

data class RoutingEvaluatedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val routingResult: RoutingResult

): PaymentEvent
{
}
