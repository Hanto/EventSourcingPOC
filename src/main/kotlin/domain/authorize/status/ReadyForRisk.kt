package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.RiskEvaluatedEvent
import domain.authorize.steps.fraud.FraudAnalysisResult
import domain.events.FraudEvaluationCompletedEvent
import domain.events.PaymentRejectedEvent
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload

data class ReadyForRisk
(
    override val newSideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload

): PaymentStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): PaymentStatus =

        when (event)
        {
            is RiskEvaluatedEvent -> apply(event, isNew)
            else -> this
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: RiskEvaluatedEvent, isNew: Boolean): PaymentStatus
    {
        val newSideEffectEvents = newSideEffectEvents.toMutableList()

        return when (event.fraudAnalysisResult)
        {
            is FraudAnalysisResult.Denied ->
            {
                newSideEffectEvents.addNewEvent(FraudEvaluationCompletedEvent, isNew)
                newSideEffectEvents.addNewEvent(PaymentRejectedEvent, isNew)

                RejectedByRisk(
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents
                )
            }

            is FraudAnalysisResult.Approved ->
            {
                newSideEffectEvents.addNewEvent(FraudEvaluationCompletedEvent, isNew)

                ReadyForRouting(
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents,
                    riskAssessmentOutcome = event.fraudAnalysisResult.riskAssessmentOutcome
                )
            }
        }
    }

    private fun MutableList<SideEffectEvent>.addNewEvent(event: SideEffectEvent, isNew: Boolean)
    {
        if (isNew)
            this.add(event)
    }
}
