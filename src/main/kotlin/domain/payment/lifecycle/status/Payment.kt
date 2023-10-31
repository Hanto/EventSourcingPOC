package domain.payment.lifecycle.status

import domain.events.SideEffectEvent
import domain.payment.Attempt
import domain.payment.AttemptReference
import domain.payment.Version
import domain.payment.Versionable
import domain.payment.lifecycle.events.PaymentEvent
import domain.payment.payload.PaymentPayload

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
