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
            is ReadyToInitiateAuthentication -> this.copy(
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
            is ReadyToInitiateAuthenticationAndAuthorization -> this.copy(
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
            is RejectedByECIVerification -> this.copy(
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
            is ReadyToReturnFromAuthentication ->  this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyToContinueAuthentication -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyToReturnFromAuthenticationAndAuthorization -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyToContinuaAuthenticationAndAuthorization -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyToEndAuthorization -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is Captured -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyToDecideAuthMethod -> this.copy(
                version = version.updateToLatestEventVersion(paymentEvents),
                paymentEvents = emptyList(),
                sideEffectEvents = sideEffectEvents
            )
            is ReadyToVerifyAuthentication -> this.copy(
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
            is ReadyToInitiateAuthenticationAndAuthorization -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyToInitiateAuthentication -> this.copy(
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
            is RejectedByECIVerification -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyToReturnFromAuthentication -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyToContinueAuthentication -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyToReturnFromAuthenticationAndAuthorization -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyToContinuaAuthenticationAndAuthorization -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyToEndAuthorization -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is Captured -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyToDecideAuthMethod -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
            is ReadyToVerifyAuthentication -> this.copy(
                version = version,
                paymentEvents = paymentEvents,
                sideEffectEvents = emptyList()
            )
        }
    }
}
