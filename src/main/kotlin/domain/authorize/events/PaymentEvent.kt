package domain.authorize.events

import domain.events.DomainEvent

sealed interface PaymentEvent : DomainEvent
{
    val version: Int
}
