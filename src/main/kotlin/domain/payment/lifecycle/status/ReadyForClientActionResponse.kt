package domain.payment.lifecycle.status

import domain.events.SideEffectEvent
import domain.events.SideEffectEventList
import domain.payment.PaymentPayload
import domain.payment.RetryAttemp
import domain.payment.Version
import domain.payment.lifecycle.events.PaymentEvent
import domain.payment.lifecycle.events.ReturnedFromClientEvent
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.gateway.ClientAction
import domain.services.routing.PaymentAccount
import java.util.logging.Logger

data class ReadyForClientActionResponse
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: RetryAttemp,
    val paymentAccount: PaymentAccount,
    val clientAction: ClientAction,

    ): AbstractPayment(), Payment
{
    private val log = Logger.getLogger(ReadyForClientActionResponse::class.java.name)

    fun addConfirmParameters(confirmParameters: Map<String, Any>): Payment
    {
        val event = ReturnedFromClientEvent(
            paymentId = payload.paymentId,
            version = version.nextEventVersion(paymentEvents),
            confirmParameters = confirmParameters)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is ReturnedFromClientEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: ReturnedFromClientEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return ReadyForConfirm(
            version = newVersion,
            paymentEvents = newEvents,
            sideEffectEvents = newSideEffectEvents.list,
            payload = payload,
            riskAssessmentOutcome = riskAssessmentOutcome,
            retryAttemps = retryAttemps,
            paymentAccount = paymentAccount,
            confirmParameters = event.confirmParameters
        )
    }
}
