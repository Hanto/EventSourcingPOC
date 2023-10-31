package domain.payment.lifecycle.status

import domain.events.*
import domain.payment.PaymentPayload
import domain.payment.RetryAttemp
import domain.payment.Version
import domain.payment.lifecycle.events.PaymentEvent
import domain.payment.lifecycle.events.RoutingEvaluatedEvent
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.routing.RoutingResult
import java.util.logging.Logger

data class ReadyForRoutingInitial
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val payload: PaymentPayload,
    override val riskAssessmentOutcome: RiskAssessmentOutcome,

    ): AbstractPayment(), Payment, ReadyForRouting
{
    private val log = Logger.getLogger(ReadyForRoutingInitial::class.java.name)

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
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                )
            }

            is RoutingResult.Proceed ->
            {
                newSideEffectEvents.addIfNew(RoutingCompletedEvent, isNew)

                ReadyForAuthorization(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemp = RetryAttemp.firstNormalAttemp(),
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