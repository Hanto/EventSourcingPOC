package domain.payment.sideeffectevents

interface EventPublisher
{
    fun publish(event: DomainEvent)
}
