package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.RiskEvaluatedEvent
import domain.authorize.steps.fraud.FraudAnalysisResult
import domain.payment.PaymentPayload
import domain.sideeffectevents.FraudEvaluationCompletedEvent
import domain.sideeffectevents.PaymentRejectedEvent
import domain.sideeffectevents.SideEffectEvent

data class ReadyForRisk
(
    override val newEvents: MutableList<SideEffectEvent>,
    override val paymentPayload: PaymentPayload

): AuthorizationStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): AuthorizationStatus =

        when (event)
        {
            is RiskEvaluatedEvent -> apply(event, isNew)
            else -> this
        }

    // RISK EVALUATED:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: RiskEvaluatedEvent, isNew: Boolean): AuthorizationStatus =

        when (event.fraudAnalysisResult)
        {
            is FraudAnalysisResult.Denied ->
            {
                addNewEvent(FraudEvaluationCompletedEvent, isNew)
                addNewEvent(PaymentRejectedEvent, isNew)

                RejectedByRisk(
                    paymentPayload = paymentPayload,
                    newEvents = newEvents)
            }

            is FraudAnalysisResult.Approved ->
            {
                addNewEvent(FraudEvaluationCompletedEvent, isNew)

                ReadyForRouting(
                    paymentPayload = paymentPayload,
                    newEvents = newEvents,
                    riskAssessmentOutcome = event.fraudAnalysisResult.riskAssessmentOutcome)
            }
        }

    private fun addNewEvent(event: SideEffectEvent, isNew: Boolean)
    {
        if (isNew)
            newEvents.add(event)
    }
}
