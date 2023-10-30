package domain.authorize.events

import domain.events.DomainEvent
import domain.payment.Version

sealed interface PaymentEvent : DomainEvent
{
    val version: Version
}
