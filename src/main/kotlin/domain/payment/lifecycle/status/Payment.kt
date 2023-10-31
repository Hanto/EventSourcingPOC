package domain.payment.lifecycle.status

import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import domain.payment.Version
import domain.payment.Versionable
import domain.payment.lifecycle.events.PaymentEvent

sealed interface Payment : Versionable
{
    override val version: Version
    val paymentEvents: List<PaymentEvent>
    val sideEffectEvents: List<SideEffectEvent>
    val payload: PaymentPayload?
    fun applyRecordedEvent(event: PaymentEvent): Payment
    fun apply(event: PaymentEvent, isNew: Boolean): Payment
    fun flushPaymentEvents(): Payment
    {
        return when(this)
        {
            is ReadyForPaymentRequest -> this

            is Authorized -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is Failed -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForAuthorization -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForClientActionResponse -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForConfirm -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForRisk -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForRoutingInitial -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForRoutingRetry -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is RejectedByGateway -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is RejectedByGatewayAndNotRetriable -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is RejectedByRisk -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is RejectedByRouting -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is RejectedByRoutingRetry -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
        }
    }

    fun flushSideEffectEvents(): Payment
    {
        return when (this)
        {
            is ReadyForPaymentRequest -> this

            is Authorized -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is Failed -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForAuthorization -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForClientActionResponse -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForConfirm -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForRisk -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForRoutingInitial -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForRoutingRetry -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByGateway -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByGatewayAndNotRetriable -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByRisk -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByRouting -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByRoutingRetry -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
        }
    }
}
