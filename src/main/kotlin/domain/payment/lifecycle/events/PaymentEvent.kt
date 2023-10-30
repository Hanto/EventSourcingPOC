package domain.payment.lifecycle.events

import domain.events.DomainEvent
import domain.payment.Version
import domain.payment.Versionable
import domain.payment.payload.PaymentId

sealed interface PaymentEvent : DomainEvent, Versionable
{
    val paymentEventId: PaymentEventId
    val paymentId: PaymentId
    override val version: Version
}
