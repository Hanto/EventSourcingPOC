package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.Version
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.paymentevents.PaymentEvent
import domain.payment.paymentevents.RiskEvaluatedEvent
import domain.payment.sideeffectevents.FraudEvaluationCompletedEvent
import domain.payment.sideeffectevents.PaymentRejectedEvent
import domain.payment.sideeffectevents.SideEffectEvent
import domain.payment.sideeffectevents.SideEffectEventList
import domain.services.fraud.FraudAnalysisResult
import java.util.logging.Logger

data class ReadyForRisk
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload

): AbstractPayment(), Payment, AuthorizeInProgress
{
    private val log = Logger.getLogger(ReadyForRisk::class.java.name)

    override fun payload(): PaymentPayload = payload
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
                    attempt = attempt,
                    payload = payload,
                )
            }

            is FraudAnalysisResult.Approved ->
            {
                ReadyForRoutingInitial(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    payload = payload,
                    attempt = attempt,
                    riskAssessmentOutcome = event.fraudAnalysisResult.riskAssessmentOutcome
                )
            }
        }
    }
}
