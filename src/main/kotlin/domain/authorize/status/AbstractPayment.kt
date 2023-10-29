package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.events.SideEffectEvent

sealed abstract class AbstractPayment : Payment
{
    override fun applyRecordedEvent(event: PaymentEvent): Payment =
        apply(event, isNew = false)

    protected fun nextVersion(): Int =
        baseVersion + paymentEvents.size + 1

    protected fun MutableList<SideEffectEvent>.addNewEvent(event: SideEffectEvent, isNew: Boolean) =
        if (isNew) { this.add(event); Unit } else Unit

    protected fun addEventIfNew(event: PaymentEvent, isNew: Boolean) =
        if (isNew) paymentEvents + event else paymentEvents

    protected fun upgradeVersionIfReplay(event: PaymentEvent, isNew: Boolean) =
        if (isNew) baseVersion else event.version

    protected fun toMutableSideEffectEvents() =
        sideEffectEvents.toMutableList()
}
