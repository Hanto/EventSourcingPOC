package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import domain.payment.Version

sealed interface Payment
{
    val baseVersion: Version
    val paymentEvents: List<PaymentEvent>
    val sideEffectEvents: List<SideEffectEvent>
    val paymentPayload: PaymentPayload?
    fun applyRecordedEvent(event: PaymentEvent): Payment
    fun apply(event: PaymentEvent, isNew: Boolean): Payment
    fun flushPaymentEvents(): Payment
    {
        return when(this)
        {
            is ReadyForPaymentRequest -> this

            is Authorized -> this.copy(
                baseVersion = baseVersion.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is Failed -> this.copy(
                baseVersion = baseVersion.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForAuthorization -> this.copy(
                baseVersion = baseVersion.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForClientActionResponse -> this.copy(
                baseVersion = baseVersion.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForConfirm -> this.copy(
                baseVersion = baseVersion.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForRisk -> this.copy(
                baseVersion = baseVersion.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForRouting -> this.copy(
                baseVersion = baseVersion.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForRoutingRetry -> this.copy(
                baseVersion = baseVersion.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is RejectedByGateway -> this.copy(
                baseVersion = baseVersion.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is RejectedByRisk -> this.copy(
                baseVersion = baseVersion.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is RejectedByRouting -> this.copy(
                baseVersion = baseVersion.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
        }
    }
}
