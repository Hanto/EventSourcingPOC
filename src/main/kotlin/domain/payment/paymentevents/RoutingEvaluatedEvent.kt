package domain.payment.paymentevents

import domain.payment.data.Version
import domain.payment.data.paymentpayload.PaymentId
import domain.services.routing.RoutingResult

data class RoutingEvaluatedEvent
(
    override val id: PaymentEventId = PaymentEventId(),
    override val paymentId: PaymentId,
    override val version: Version,
    val routingResult: RoutingResult

): PaymentEvent
{
}
