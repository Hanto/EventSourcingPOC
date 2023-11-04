package domain.payment.state

import domain.payment.data.AttemptReference
import domain.payment.paymentevents.PaymentEvent

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
            is ReadyForAuthentication -> this.copy(
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
            is ReadyForAuthorization -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is RejectedByAuthentication -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is RejectedByAuthorization -> this.copy(
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
            is RejectedByRoutingSameAccount -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForAuthenticationClientAction ->  this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForAuthenticationConfirm -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForAuthenticationAndAuthorizeClientAction -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyForAuthenticationAndAuthorizeConfirm -> this.copy(
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
            is ReadyForAuthentication -> this.copy(
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
            is ReadyForAuthorization -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByAuthentication -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is RejectedByAuthorization -> this.copy(
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
            is RejectedByRoutingSameAccount -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForAuthenticationClientAction -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForAuthenticationConfirm -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForAuthenticationAndAuthorizeClientAction -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyForAuthenticationAndAuthorizeConfirm -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
        }
    }
}
