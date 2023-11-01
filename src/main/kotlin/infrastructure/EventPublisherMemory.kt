package infrastructure

import domain.payment.sideeffectevents.DomainEvent
import domain.payment.sideeffectevents.EventPublisher

class EventPublisherMemory : EventPublisher
{
    val list: MutableList<DomainEvent> = mutableListOf()

    override fun publish(event: DomainEvent)
    {
        list.add(event)
    }
}
