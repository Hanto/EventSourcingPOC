package domain.authorize.events

import domain.events.DomainEvent
import domain.payment.PaymentId
import domain.payment.Version

sealed interface PaymentEvent : DomainEvent
{
    val paymentEventId: PaymentEventId
    val paymentId: PaymentId
    val version: Version
}
