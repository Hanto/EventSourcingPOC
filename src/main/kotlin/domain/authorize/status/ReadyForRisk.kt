package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.RiskEvaluatedEvent
import domain.authorize.steps.fraud.FraudAnalysisResult
import domain.events.FraudEvaluationCompletedEvent
import domain.events.PaymentRejectedEvent
import domain.events.SideEffectEvent
import domain.events.SideEffectEventList
import domain.payment.PaymentPayload
import domain.payment.Version
import java.util.logging.Logger

data class ReadyForRisk
(
    override val baseVersion: Version,
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
            version = baseVersion.nextEventVersion(paymentEvents),
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
        val newVersion = baseVersion.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        newSideEffectEvents.addIfNew(FraudEvaluationCompletedEvent, isNew)

        return when (event.fraudAnalysisResult)
        {
            is FraudAnalysisResult.Denied ->
            {
                newSideEffectEvents.addIfNew(PaymentRejectedEvent, isNew)

                RejectedByRisk(
                    baseVersion = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    payload = payload,
                )
            }

            is FraudAnalysisResult.Approved ->
            {
                ReadyForRouting(
                    baseVersion = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    payload = payload,
                    riskAssessmentOutcome = event.fraudAnalysisResult.riskAssessmentOutcome
                )
            }
        }
    }
}
