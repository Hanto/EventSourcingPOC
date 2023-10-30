package domain.payment.lifecycle.events

import domain.payment.Version
import domain.payment.payload.PaymentId
import domain.services.routing.RoutingResult

data class RoutingEvaluatedEvent
(
    override val paymentEventId: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val routingResult: RoutingResult

): PaymentEvent
{
}
