package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.payment.PaymentPayload
import domain.sideeffectevents.SideEffectEvent

data class Failed
(
    override val newEvents: MutableList<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val reason: String,

    ): AuthorizationStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): AuthorizationStatus = this
}
