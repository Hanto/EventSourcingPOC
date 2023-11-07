package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.AuthorisationAction
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.data.paymentpayload.paymentmethod.CreditCardPayment
import domain.payment.data.paymentpayload.paymentmethod.KlarnaPayment
import domain.payment.data.paymentpayload.paymentmethod.PayPalPayment
import domain.payment.paymentevents.PaymentEvent
import domain.payment.paymentevents.RoutingActionEvaluatedEVent
import domain.payment.sideeffectevents.SideEffectEvent
import domain.payment.sideeffectevents.SideEffectEventList
import domain.services.gateway.AuthenticateOutcome
import java.util.logging.Logger

data class ReadyToDecideAuthMethod
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount,

) : AbstractPayment(), Payment
{
    private val log = Logger.getLogger(ReadyForRoutingRetry::class.java.name)

    override fun payload(): PaymentPayload = payload
    fun decideAuthMethod(decouplingEnabled: Boolean): Payment
    {
        val event = RoutingActionEvaluatedEVent(
            paymentId = payload.id,
            version = version.nextEventVersion(paymentEvents),
            decuplingEnabled = decouplingEnabled)

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is RoutingActionEvaluatedEVent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: RoutingActionEvaluatedEVent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        if (!event.decuplingEnabled)
        {
            return ReadyForAuthenticationAndAuthorization(
                version = newVersion,
                paymentEvents = newEvents,
                sideEffectEvents = newSideEffectEvents.list,
                attempt = attempt,
                payload = payload,
                riskAssessmentOutcome = riskAssessmentOutcome,
                paymentAccount = paymentAccount
            )
        }

        return when(payload.paymentMethod)
        {
            KlarnaPayment, PayPalPayment ->
            {
                return ReadyForAuthenticationAndAuthorization(
                    version = newVersion,
                    paymentEvents = newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount
                )
            }

            CreditCardPayment ->
            {
                when(paymentAccount.authorisationAction)
                {
                    is AuthorisationAction.ThreeDS ->
                    {
                        ReadyForAuthentication(
                            version = newVersion,
                            paymentEvents = newEvents,
                            sideEffectEvents = newSideEffectEvents.list,
                            attempt = attempt,
                            payload = payload,
                            riskAssessmentOutcome = riskAssessmentOutcome,
                            paymentAccount = paymentAccount
                        )
                    }

                    is AuthorisationAction.Moto ->
                    {
                        ReadyForAuthorization(
                            version = newVersion,
                            paymentEvents= newEvents,
                            sideEffectEvents = newSideEffectEvents.list,
                            attempt = attempt,
                            payload = payload,
                            riskAssessmentOutcome = riskAssessmentOutcome,
                            paymentAccount = paymentAccount,
                            authenticateOutcome = AuthenticateOutcome.Skipped
                        )
                    }

                    is AuthorisationAction.Ecommerce ->
                    {
                        ReadyForAuthorization(
                            version = newVersion,
                            paymentEvents= newEvents,
                            sideEffectEvents = newSideEffectEvents.list,
                            attempt = attempt,
                            payload = payload,
                            riskAssessmentOutcome = riskAssessmentOutcome,
                            paymentAccount = paymentAccount,
                            authenticateOutcome = AuthenticateOutcome.Skipped
                        )
                    }
                }
            }
        }
    }
}
