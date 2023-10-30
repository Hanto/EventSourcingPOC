package domain.authorize.status

import domain.authorize.events.PaymentEvent

sealed abstract class AbstractPayment : Payment
{
    override fun applyRecordedEvent(event: PaymentEvent): Payment =
        apply(event, isNew = false)

    protected fun addEventIfNew(event: PaymentEvent, isNew: Boolean) =
        if (isNew) paymentEvents + event else paymentEvents
}
