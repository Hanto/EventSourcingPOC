package domain.authorize.status

import domain.authorize.events.PaymentEvent
import domain.authorize.events.ReturnedFromClient
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.gateway.ClientAction
import domain.authorize.steps.routing.PaymentAccount
import domain.events.SideEffectEvent
import domain.payment.PaymentPayload
import java.util.logging.Logger

data class ReadyForClientActionResponse
(
    override val baseVersion: Int,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val paymentPayload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val retryAttemps: Int,
    val paymentAccount: PaymentAccount,
    val clientAction: ClientAction,

    ): AbstractPayment(), Payment
{
    private val log = Logger.getLogger(ReadyForClientActionResponse::class.java.name)

    fun addConfirmParameters(confirmParameters: Map<String, Any>): Payment
    {
        val event = ReturnedFromClient(
            version = nextVersion(),
            confirmParameters = confirmParameters)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is ReturnedFromClient -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: ReturnedFromClient, isNew: Boolean): Payment
    {
        val newEvents = addEventIfNew(event, isNew)
        val newVersion = upgradeVersionIfReplay(event, isNew)
        val newSideEffectEvents = toMutableSideEffectEvents()

        return ReadyForConfirm(
            baseVersion = newVersion,
            paymentEvents = newEvents,
            sideEffectEvents = newSideEffectEvents,
            paymentPayload = paymentPayload,
            riskAssessmentOutcome = riskAssessmentOutcome,
            retryAttemps = retryAttemps,
            paymentAccount = paymentAccount,
            confirmParameters = event.confirmParameters
        )
    }
}
