package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.AttemptReference
import domain.payment.data.Version
import domain.payment.data.Versionable
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.SideEffectEvent

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
