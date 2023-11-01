package domain.payment.lifecycle.status

import domain.payment.AttemptReference
import domain.payment.lifecycle.events.PaymentEvent

sealed class AbstractPayment : Payment
{
    override fun applyRecordedEvent(event: PaymentEvent): Payment =
        apply(event, isNew = false)

    override fun attemptReference(): AttemptReference =
        attempt.generateAttemptReference(this)

    protected fun addEventIfNew(event: PaymentEvent, isNew: Boolean) =
        if (isNew) paymentEvents + event else paymentEvents

    override fun flushPaymentEvents(): Payment
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

    override fun flushSideEffectEvents(): Payment
    {
        return when (this)
        {
            is ReadyForPaymentRequest -> this

            is Authorized -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is Failed -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForAuthorization -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForClientActionResponse -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForConfirm -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForRisk -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForRoutingInitial -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForRoutingRetry -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByGateway -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByGatewayAndNotRetriable -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByRisk -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByRouting -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByRoutingRetry -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
        }
    }
}
