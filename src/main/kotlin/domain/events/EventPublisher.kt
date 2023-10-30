package domain.events

interface EventPublisher
{
    fun publish(event: DomainEvent)
}
