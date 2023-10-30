package infrastructure

import domain.events.DomainEvent
import domain.events.EventPublisher

class EventPublisherMemory : EventPublisher
{
    val list: MutableList<DomainEvent> = mutableListOf()

    override fun publish(event: DomainEvent)
    {
        list.add(event)
    }
}
