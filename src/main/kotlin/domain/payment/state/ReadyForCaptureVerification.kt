package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.AuthorizationType
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.paymentevents.PaymentCapturedCheckEvent
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.PaymentSettledEvent
import domain.payment.sideeffectevents.SideEffectEvent
import domain.payment.sideeffectevents.SideEffectEventList
import domain.services.gateway.AuthenticateResponse
import domain.services.gateway.AuthorizeResponse
import java.util.logging.Logger

data class ReadyForCaptureVerification
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount,
    val authenticateResponse: AuthenticateResponse,
    val authorizeResponse: AuthorizeResponse.AuthorizeSuccess,

): AbstractPayment(), Payment
{
    private val log = Logger.getLogger(ReadyForCaptureVerification::class.java.name)

    override fun payload(): PaymentPayload = payload

    fun checkIfPaymentCaptured(): Payment
    {
        val event = PaymentCapturedCheckEvent(
            paymentId = payload.id,
            version = version.nextEventVersion(paymentEvents)
        )

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is PaymentCapturedCheckEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: PaymentCapturedCheckEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return when (payload.authorizationType)
        {
            AuthorizationType.FULL_AUTHORIZATION ->
            {
                newSideEffectEvents.addIfNew(PaymentSettledEvent, isNew)

                Captured(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateResponse = authenticateResponse,
                    authorizeResponse = authorizeResponse
                )
            }
            AuthorizationType.PRE_AUTHORIZATION ->
            {
                Authorized(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateResponse = authenticateResponse,
                    authorizeResponse = authorizeResponse
                )
            }
        }
    }
}

