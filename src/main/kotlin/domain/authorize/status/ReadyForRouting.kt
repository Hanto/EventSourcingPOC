package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.RoutingEvaluatedEvent
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.routing.RoutingResult
import domain.events.PaymentFailedEvent
import domain.events.PaymentRejectedEvent
import domain.events.RoutingCompletedEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import domain.payment.RetryAttemp
import domain.payment.SideEffectEventList
import domain.payment.Version
import java.util.logging.Logger

data class ReadyForRouting
(
    override val baseVersion: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    override val riskAssessmentOutcome: RiskAssessmentOutcome,

    ): AbstractPayment(), Payment, ReadyForAnyRouting
{
    private val log = Logger.getLogger(ReadyForRouting::class.java.name)

    override fun addRoutingResult(routingResult: RoutingResult): Payment
    {
        val event = RoutingEvaluatedEvent(
            version = baseVersion.nextEventVersion(paymentEvents),
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
        val newVersion = baseVersion.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return when (event.routingResult)
        {
            is RoutingResult.RoutingError ->
            {
                newSideEffectEvents.addIfNew(PaymentFailedEvent, isNew)

                Failed(
                    baseVersion = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    paymentPayload = paymentPayload,
                    reason = createRoutingErrorReason(event.routingResult))
            }

            is RoutingResult.Reject ->
            {
                newSideEffectEvents.addIfNew(RoutingCompletedEvent, isNew)
                newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

                RejectedByRouting(
                    baseVersion = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    paymentPayload = paymentPayload,
                )
            }

            is RoutingResult.Proceed ->
            {
                newSideEffectEvents.addIfNew(RoutingCompletedEvent, isNew)

                ReadyForAuthorization(
                    baseVersion = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    paymentPayload = paymentPayload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    retryAttemps = RetryAttemp.firstNormalAttemp(),
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
