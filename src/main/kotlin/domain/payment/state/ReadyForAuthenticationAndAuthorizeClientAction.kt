package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.paymentevents.PaymentEvent
import domain.payment.paymentevents.ReturnedFromClientEvent
import domain.payment.sideeffectevents.SideEffectEvent
import domain.payment.sideeffectevents.SideEffectEventList
import domain.services.gateway.AuthenticateResponse

data class ReadyForAuthenticationAndAuthorizeClientAction
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount,
    val authenticateResponse: AuthenticateResponse.AuthenticateAndAuthorizeClientAction,

): AbstractPayment(), Payment, AuthorizeInProgress
{
    override fun payload(): PaymentPayload = payload

    fun addConfirmParameters(confirmParameters: Map<String, Any>): Payment
    {
        val event = ReturnedFromClientEvent(
            paymentId = payload.id,
            version = version.nextEventVersion(paymentEvents),
            confirmParameters = confirmParameters)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return ReadyForAuthenticationAndAuthorizeConfirm(
            version = newVersion,
            paymentEvents = newEvents,
            sideEffectEvents = newSideEffectEvents.list,
            payload = payload,
            riskAssessmentOutcome = riskAssessmentOutcome,
            attempt = attempt,
            paymentAccount = paymentAccount,
            authenticateResponse = authenticateResponse,
        )
    }
}
