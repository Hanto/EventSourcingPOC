package domain.payment.paymentevents

import domain.payment.data.Version
import domain.payment.data.Versionable
import domain.payment.data.paymentpayload.PaymentId
import domain.payment.sideeffectevents.DomainEvent

sealed interface PaymentEvent : DomainEvent, Versionable
{
    val paymentEventId: PaymentEventId
    val paymentId: PaymentId
    override val version: Version
}
