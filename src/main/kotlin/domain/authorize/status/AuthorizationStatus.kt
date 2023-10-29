package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.payment.PaymentPayload
import domain.sideeffectevents.SideEffectEvent

sealed interface AuthorizationStatus
{
    val newSideEffectEvents: MutableList<SideEffectEvent>
    val paymentPayload: PaymentPayload?
    fun apply(event: PaymentEvent, isNew: Boolean): AuthorizationStatus
}
