package infrastructure.repositories.paymentrepositoryold

import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.paymentpayload.AuthorizationType
import domain.payment.data.threedstatus.ExemptionStatus
import domain.payment.data.threedstatus.ThreeDSStatus
import domain.payment.paymentevents.PaymentEvent
import domain.payment.paymentevents.RiskEvaluatedEvent
import domain.payment.state.*
import domain.services.fraud.FraudAnalysisResult
import infrastructure.repositories.paymentrepositoryold.paymentdata.AuthPaymentOperation
import infrastructure.repositories.paymentrepositoryold.paymentdata.AuthPaymentOperation.Exemption
import infrastructure.repositories.paymentrepositoryold.paymentdata.PaymentData

class PaymentAdapter
{
    fun toPaymentData(payment: Payment, events: List<PaymentEvent>): PaymentData
    {
        return PaymentData(
            id = payment.payload().id.value,
            authorizationReference = payment.payload().authorizationReference.value,
            operations = retrieveOperations(events),
            riskAssessmentOutcome = retrieveRiskAssessment(events)
        )
    }

    // FIELDS:
    //------------------------------------------------------------------------------------------------------------------

    private fun retrieveOperations(events: List<PaymentEvent>): List<AuthPaymentOperation>
    {
        val operations: MutableList<AuthPaymentOperation> = mutableListOf()

        events.foldIndexed(ReadyForPaymentRequest() as Payment) { index, previousState, event ->

            val currentState = previousState.apply(event, false)
            val isLastEvent = isLastEvent(index, events)

            addOperation(currentState, operations, isLastEvent)

            currentState
        }
        return operations
    }

    private fun retrieveRiskAssessment(events: List<PaymentEvent>): RiskAssessmentOutcome? =

        events
            .filterIsInstance<RiskEvaluatedEvent>()
            .map { it.fraudAnalysisResult.toRiskAssessmentOutcome() }
            .firstOrNull()

    private fun isLastEvent(index: Int, events: List<PaymentEvent>) = index == events.size - 1

    // OPERATIONS:
    //------------------------------------------------------------------------------------------------------------------

    private fun addOperation(payment: Payment, operations: MutableList<AuthPaymentOperation>, isLastEvent: Boolean)
    {
        when (payment)
        {
            // IN PROGRESS STATES:
            //----------------------------------------------------------------------------------------------------------

            is ReadyForPaymentRequest -> null
            is ReadyForRisk -> null
            is ReadyForRoutingInitial -> null
            is ReadyForAuthenticationConfirm -> null
            is ReadyForAuthenticationAndAuthorizeConfirm -> null
            is ReadyForRoutingRetry -> null
            is ReadyForAuthorization -> if (isLastEvent) AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = null,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.NOT_APPLICABLE,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.PENDING,
                paymentClassName = payment.toPaymentClassName()
            ) else null
            is ReadyForAuthentication -> if (isLastEvent) AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = null,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.NOT_APPLICABLE,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.PENDING,
                paymentClassName = payment.toPaymentClassName()
            ) else null

            // PENDING STATES:
            //----------------------------------------------------------------------------------------------------------

            is ReadyForAuthenticationClientAction -> if (isLastEvent) AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = payment.authenticateResponse.pspReference.value,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateResponse.threeDSStatus.toECI(),
                exemption = payment.authenticateResponse.threeDSStatus.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.PENDING,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.PENDING,
                paymentClassName = payment.toPaymentClassName()
            ) else null

            is ReadyForAuthenticationAndAuthorizeClientAction -> if (isLastEvent) AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = payment.authenticateResponse.pspReference.value,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateResponse.threeDSStatus.toECI(),
                exemption = payment.authenticateResponse.threeDSStatus.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.PENDING,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.PENDING,
                paymentClassName = payment.toPaymentClassName()
            ) else null

            // FINAL STATES:
            //----------------------------------------------------------------------------------------------------------

            is RejectedByGatewayAndNotRetriable -> null

            is Authorized -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = payment.authorizeResponse.pspReference.value,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateResponse.threeDSStatus.toECI(),
                exemption = payment.authenticateResponse.threeDSStatus.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.OK,
                paymentClassName = payment.toPaymentClassName()
            )
            is Failed -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = payment.authorizeResponse?.pspReference?.value ?: payment.authenticateResponse?.pspReference?.value,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateResponse?.threeDSStatus?.toECI(),
                exemption = payment.authenticateResponse?.threeDSStatus.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.KO,
                paymentClassName = payment.toPaymentClassName()
            )
            is RejectedByAuthentication -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = payment.authenticateResponse.pspReference.value,
                reference =payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateResponse.threeDSStatus.toECI(),
                exemption = payment.authenticateResponse.threeDSStatus.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.KO,
                paymentClassName = payment.toPaymentClassName()
            )
            is RejectedByAuthorization -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = payment.authorizeResponse.pspReference.value,
                reference =payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateResponse.threeDSStatus.toECI(),
                exemption = payment.authenticateResponse.threeDSStatus.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.KO,
                paymentClassName = payment.toPaymentClassName()
            )
            is RejectedByRisk -> AuthPaymentOperation(
                paymentAccount = null,
                pspReference = null,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.KO,
                paymentClassName = payment.toPaymentClassName()
            )
            is RejectedByRouting -> AuthPaymentOperation(
                paymentAccount = null,
                pspReference = null,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.KO,
                paymentClassName = payment.toPaymentClassName()
            )
            is RejectedByRoutingSameAccount -> null

        }?.let { operations.add(it) }
    }

    // ADAPTERS:
    //------------------------------------------------------------------------------------------------------------------

    private fun FraudAnalysisResult.toRiskAssessmentOutcome() =

        when (this)
        {
            is FraudAnalysisResult.Approved -> this.riskAssessmentOutcome
            FraudAnalysisResult.Denied -> null
        }

    private fun ThreeDSStatus?.toExemption() =

        when (this)
        {
            null -> Exemption.NotRequested
            is ThreeDSStatus.NoThreeDS -> Exemption.NotRequested
            is ThreeDSStatus.PendingThreeDS -> Exemption.NotRequested
            is ThreeDSStatus.ThreeDS -> when (this.exemptionStatus)
            {
                is ExemptionStatus.ExemptionNotRequested -> Exemption.NotRequested
                is ExemptionStatus.ExemptionAccepted -> Exemption.Accepted
                is ExemptionStatus.ExemptionNotAccepted -> Exemption.NotAccepted
            }
        }

    private fun ThreeDSStatus.toECI() =

        when (this)
        {
            is ThreeDSStatus.NoThreeDS -> null
            is ThreeDSStatus.PendingThreeDS -> null
            is ThreeDSStatus.ThreeDS -> this.eci.value.toString()
        }

    private fun AuthorizationType.toTransactionType() =

        when (this)
        {
            AuthorizationType.FULL_AUTHORIZATION -> TransactionType.AUTHORIZE_AND_SETTLE
            AuthorizationType.PRE_AUTHORIZATION -> TransactionType.AUTHORIZE
        }

    private fun Payment.toPaymentClassName() =
        this::class.java.simpleName
}
