package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.PaymentRequestedEvent
import domain.payment.PaymentPayload
import domain.sideeffectevents.SideEffectEvent

class ReadyForPaymentRequest : AuthorizationStatus
{
    override val paymentPayload: PaymentPayload? = null
    override val newSideEffectEvents: MutableList<SideEffectEvent> = mutableListOf()

    override fun apply(event: PaymentEvent, isNew: Boolean): AuthorizationStatus =

        when (event)
        {
            is PaymentRequestedEvent -> apply(event, isNew)
            else -> this
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: PaymentRequestedEvent, isNew: Boolean): AuthorizationStatus =

        ReadyForRisk(
            paymentPayload = event.paymentPayload,
            newSideEffectEvents = newSideEffectEvents
        )
}
