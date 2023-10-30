package domain.authorize.events

import domain.events.DomainEvent
import domain.payment.Version
import domain.payment.payload.PaymentId

sealed interface PaymentEvent : DomainEvent
{
    val paymentEventId: PaymentEventId
    val paymentId: PaymentId
    val version: Version
}
