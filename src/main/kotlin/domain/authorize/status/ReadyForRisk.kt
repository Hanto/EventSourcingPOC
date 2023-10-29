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
    override val newSideEffectEvents: MutableList<SideEffectEvent>,
    override val paymentPayload: PaymentPayload

): AuthorizationStatus
{
    override fun apply(event: PaymentEvent, isNew: Boolean): AuthorizationStatus =

        when (event)
        {
            is RiskEvaluatedEvent -> apply(event, isNew)
            else -> this
        }

    // APPLY EVENT:
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
                    newSideEffectEvents = newSideEffectEvents
                )
            }

            is FraudAnalysisResult.Approved ->
            {
                addNewEvent(FraudEvaluationCompletedEvent, isNew)

                ReadyForRouting(
                    paymentPayload = paymentPayload,
                    newSideEffectEvents = newSideEffectEvents,
                    riskAssessmentOutcome = event.fraudAnalysisResult.riskAssessmentOutcome
                )
            }
        }

    private fun addNewEvent(event: SideEffectEvent, isNew: Boolean)
    {
        if (isNew)
            newSideEffectEvents.add(event)
    }
}
