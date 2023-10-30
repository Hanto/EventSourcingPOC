package domain.lifecycle.status

import domain.events.FraudEvaluationCompletedEvent
import domain.events.PaymentRejectedEvent
import domain.events.SideEffectEvent
import domain.events.SideEffectEventList
import domain.lifecycle.events.PaymentEvent
import domain.lifecycle.events.RiskEvaluatedEvent
import domain.lifecycle.steps.fraud.FraudAnalysisResult
import domain.payment.PaymentPayload
import domain.payment.Version
import java.util.logging.Logger

data class ReadyForRisk
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val payload: PaymentPayload

): AbstractPayment(), Payment
{
    private val log = Logger.getLogger(ReadyForRisk::class.java.name)

    fun addFraudAnalysisResult(fraudAnalysisResult: FraudAnalysisResult): Payment
    {
        val event = RiskEvaluatedEvent(
            paymentId = payload.paymentId,
            version = version.nextEventVersion(paymentEvents),
            fraudAnalysisResult = fraudAnalysisResult)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is RiskEvaluatedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: RiskEvaluatedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        newSideEffectEvents.addIfNew(FraudEvaluationCompletedEvent, isNew)

        return when (event.fraudAnalysisResult)
        {
            is FraudAnalysisResult.Denied ->
            {
                newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

                RejectedByRisk(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    payload = payload,
                )
            }

            is FraudAnalysisResult.Approved ->
            {
                ReadyForRouting(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    payload = payload,
                    riskAssessmentOutcome = event.fraudAnalysisResult.riskAssessmentOutcome
                )
            }
        }
    }
}
