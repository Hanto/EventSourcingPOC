package domain.payment.state

import domain.payment.data.Attempt
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.AuthorisationAction
import domain.payment.data.paymentaccount.AuthorisationAction.AuthorizationPreference.ECI_CHECK
import domain.payment.data.paymentaccount.AuthorisationAction.AuthorizationPreference.NO_PREFERENCE
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.data.threedstatus.ECI
import domain.payment.data.threedstatus.ExemptionStatus
import domain.payment.data.threedstatus.ThreeDSStatus
import domain.payment.paymentevents.ECIVerifiedEvent
import domain.payment.paymentevents.PaymentEvent
import domain.payment.sideeffectevents.AuthorizationAttemptRejectedEvent
import domain.payment.sideeffectevents.PaymentAuthenticationCompletedEvent
import domain.payment.sideeffectevents.SideEffectEvent
import domain.payment.sideeffectevents.SideEffectEventList
import domain.services.gateway.AuthenticateResponse
import java.util.logging.Logger

data class ReadyForECIVerfication
(
    override val version: Version,
    override val paymentEvents: List<PaymentEvent>,
    override val sideEffectEvents: List<SideEffectEvent>,
    override val attempt: Attempt,
    val payload: PaymentPayload,
    val riskAssessmentOutcome: RiskAssessmentOutcome,
    val paymentAccount: PaymentAccount,
    val authenticateOutcome: AuthenticateResponse.AuthenticateSuccess,

): AbstractPayment(), Payment
{
    private val log = Logger.getLogger(ReadyForECIVerfication::class.java.name)

    override fun payload(): PaymentPayload = payload
    fun verifyECI(): Payment
    {
        val event = ECIVerifiedEvent(
            paymentId = payload.id,
            version = version.nextEventVersion(paymentEvents))

        return apply(event, isNew = true)
    }

    override fun apply(event: PaymentEvent, isNew: Boolean): Payment =

        when (event)
        {
            is ECIVerifiedEvent -> apply(event, isNew)
            else -> { log.warning("invalid event type: ${event::class.java.simpleName}"); this }
        }

    // APPLY EVENT:
    //------------------------------------------------------------------------------------------------------------------

    private fun apply(event: ECIVerifiedEvent, isNew: Boolean): Payment
    {
        val newVersion = version.updateToEventVersionIfReplay(event, isNew)
        val newEvents = addEventIfNew(event, isNew)
        val newSideEffectEvents = SideEffectEventList(sideEffectEvents)

        return when(authenticateOutcome.exemptionStatus)
        {
            ExemptionStatus.ExemptionAccepted ->
            {
                ReadyForAuthorization(
                    version = newVersion,
                    paymentEvents= newEvents,
                    sideEffectEvents = newSideEffectEvents.list,
                    attempt = attempt,
                    payload = payload,
                    riskAssessmentOutcome = riskAssessmentOutcome,
                    paymentAccount = paymentAccount,
                    authenticateOutcome = authenticateOutcome
                )
            }
            else -> when (authenticateOutcome.threeDSStatus)
            {
                ThreeDSStatus.NoThreeDS ->
                {
                    ReadyForAuthorization(
                        version = newVersion,
                        paymentEvents= newEvents,
                        sideEffectEvents = newSideEffectEvents.list,
                        attempt = attempt,
                        payload = payload,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        paymentAccount = paymentAccount,
                        authenticateOutcome = authenticateOutcome
                    )
                }

                is ThreeDSStatus.PendingThreeDS ->
                {
                    newSideEffectEvents.addIfNew(AuthorizationAttemptRejectedEvent, isNew)
                    newSideEffectEvents.addIfNew(PaymentAuthenticationCompletedEvent, isNew)

                    Failed(
                        version = newVersion,
                        paymentEvents= newEvents,
                        sideEffectEvents = newSideEffectEvents.list,
                        attempt = attempt,
                        payload = payload,
                        riskAssessmentOutcome = riskAssessmentOutcome,
                        paymentAccount = paymentAccount,
                        authenticateOutcome = authenticateOutcome,
                        authorizeOutcome = null,
                        reason = "The final authenticate response cannot be pending"
                    )
                }

                is ThreeDSStatus.ThreeDS ->
                {
                    val isEciCheckForced = paymentAccount.isEciCheckForced()
                    val eci = authenticateOutcome.threeDSStatus.eci.result()

                    when
                    {
                        isEciCheckForced && eci != ECI.EciResult.SUCCESSFUL ->
                        {
                            newSideEffectEvents.addIfNew(AuthorizationAttemptRejectedEvent, isNew)
                            newSideEffectEvents.addIfNew(PaymentAuthenticationCompletedEvent, isNew)

                            RejectedByECIVerification(
                                version = newVersion,
                                paymentEvents= newEvents,
                                sideEffectEvents = newSideEffectEvents.list,
                                attempt = attempt,
                                payload = payload,
                                riskAssessmentOutcome = riskAssessmentOutcome,
                                paymentAccount = paymentAccount,
                                authenticateOutcome = authenticateOutcome
                            )

                        }
                        else ->
                        {
                            ReadyForAuthorization(
                                version = newVersion,
                                paymentEvents= newEvents,
                                sideEffectEvents = newSideEffectEvents.list,
                                attempt = attempt,
                                payload = payload,
                                riskAssessmentOutcome = riskAssessmentOutcome,
                                paymentAccount = paymentAccount,
                                authenticateOutcome = authenticateOutcome
                            )
                        }
                    }
                }
            }
        }
    }

    private fun PaymentAccount.isEciCheckForced(): Boolean =

        when (this.authorisationAction)
        {
            AuthorisationAction.Moto -> false
            is AuthorisationAction.NoMoto -> when (this.authorisationAction.authorizationPreference)
            {
                ECI_CHECK -> true
                NO_PREFERENCE -> false
            }
        }
}
