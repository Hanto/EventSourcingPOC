package domain.lifecycle.status

import domain.events.*
import domain.lifecycle.events.PaymentEvent
import domain.lifecycle.events.RoutingEvaluatedEvent
import domain.lifecycle.steps.fraud.RiskAssessmentOutcome
import domain.lifecycle.steps.routing.PaymentAccount
import domain.lifecycle.steps.routing.RoutingResult
import domain.payment.PaymentPayload
import domain.payment.RetryAttemp
import domain.payment.Version
import java.util.logging.Logger

data class ReadyForRoutingRetry
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val payload: PaymentPayload,
    override val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: RetryAttemp,
    val paymentAccount: PaymentAccount

) : AbstractPayment(), Payment, Routed
{
    private val log = Logger.getLogger(ReadyForRoutingRetry::class.java.name)

    override fun addRoutingResult(routingResult: RoutingResult): Payment
    {
        val event = RoutingEvaluatedEvent(
            paymentId = payload.paymentId,
            version = version.nextEventVersion(paymentEvents),
            routingResult = routingResult)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is RoutingEvaluatedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: RoutingEvaluatedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return when (event.routingResult)
        {
            is RoutingResult.RoutingError ->
            {
                newSideEffectEvents.addIfNew(RoutingCompletedEvent, isNew)
                newSideEffectEvents.addIfNew(PaymentFailedEvent, isNew)

                Failed(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    payload = payload,
                    reason = createRoutingErrorReason(event.routingResult))
            }

            is RoutingResult.Reject ->
            {
                newSideEffectEvents.addIfNew(RoutingCompletedEvent, isNew)
                newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

                RejectedByRouting(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    payload = payload)
            }

            is RoutingResult.Proceed ->
            {
                newSideEffectEvents.addIfNew(RoutingCompletedEvent, isNew)

                if (event.routingResult.account == paymentAccount)
                {
                    newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

                    RejectedByGateway(
                        version = newVersion,
                        paymentEvents = newEvents,
                        sideEffectEvents = newSideEffectEvents.list,
                        payload = payload,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        retryAttemps = retryAttemps,
                        paymentAccount = event.routingResult.account
                    )
                }

                else
                    ReadyForAuthorization(
                        version = newVersion,
                        paymentEvents = newEvents,
                        sideEffectEvents = newSideEffectEvents.list,
                        payload = payload,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        retryAttemp = retryAttemps,
                        paymentAccount = event.routingResult.account
                    )
            }
        }
    }

    private fun createRoutingErrorReason(routingError: RoutingResult.RoutingError): String =

        when (routingError)
        {
            is RoutingResult.RoutingError.InvalidCurrency -> "Currency not accepted"
            is RoutingResult.RoutingError.BankAccountNotFound -> "Unable to find bank account"
        }
}
