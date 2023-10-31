package domain.payment.lifecycle.status

import domain.events.SideEffectEvent
import domain.payment.*
import domain.payment.lifecycle.events.PaymentEvent

sealed interface Payment : Versionable
{
    override val version: Version
    val paymentEvents: List<PaymentEvent>
    val sideEffectEvents: List<SideEffectEvent>
    val attempt: Attempt

    fun payload(): PaymentPayload
    fun applyRecordedEvent(event: PaymentEvent): Payment
    fun apply(event: PaymentEvent, isNew: Boolean): Payment
    fun attemptReference(): AttemptReference
    fun flushPaymentEvents(): Payment
    fun flushSideEffectEvents(): Payment
}
