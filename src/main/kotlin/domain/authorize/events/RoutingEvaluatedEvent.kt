package domain.authorize.events

import domain.authorize.steps.routing.RoutingResult
import domain.payment.Version
import domain.payment.payload.PaymentId

data class RoutingEvaluatedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val routingResult: RoutingResult

): PaymentEvent
{
}
