package infrastructure.paymentdata

import domain.payment.lifecycle.events.PaymentEvent
import domain.payment.lifecycle.events.RiskEvaluatedEvent
import domain.payment.lifecycle.status.*
import domain.payment.payload.AuthorizationType
import domain.services.fraud.FraudAnalysisResult
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.gateway.ExemptionStatus
import domain.services.gateway.ThreeDSStatus

class PaymentAdapter
{
    fun toPaymentData(payment: Payment, events: List<PaymentEvent>): PaymentData
    {
        return PaymentData(
            id = payment.payload().paymentId.value,
            authorizationReference = payment.payload().authorizationReference.value,
            operations = retrieveOperations(events),
            riskAssessmentOutcome = retrieveRiskAssessment(events)
        )
    }

    // FIELDS:
    //------------------------------------------------------------------------------------------------------------------

    private fun retrieveRiskAssessment(events: List<PaymentEvent>): RiskAssessmentOutcome? =

        events
            .filterIsInstance<RiskEvaluatedEvent>()
            .map { it.fraudAnalysisResult.toRiskAssessmentOutcome() }
            .firstOrNull()

    private fun retrieveOperations(events: List<PaymentEvent>): List<AuthPaymentOperation>
    {
        val operations: MutableList<AuthPaymentOperation> = mutableListOf()

        events.foldIndexed(ReadyForPaymentRequest() as Payment) { index, previousState, event ->

            val currentState = previousState.apply(event, false)

            when (currentState)
            {
                is AuthorizeInProgress -> if (isLastEvent(index, events)) addOperation(currentState, operations)
                is AuthorizePending -> if (isLastEvent(index, events)) addOperation(currentState, operations)
                is AuthorizeEnded -> addOperation(currentState, operations)
            }
            print(currentState)
            currentState
        }
        return operations
    }

    private fun isLastEvent(index: Int, events: List<PaymentEvent>) = index == events.size - 1

    // OPERATIONS:
    //------------------------------------------------------------------------------------------------------------------

    private fun addOperation(payment: AuthorizeInProgress, operations: MutableList<AuthPaymentOperation>)
    {
        when (payment)
        {
            is ReadyForPaymentRequest -> null
            is ReadyForRisk -> null
            is ReadyForRoutingInitial -> null
            is ReadyForConfirm -> null
            is ReadyForRoutingRetry -> null
            is ReadyForAuthorization -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = AuthPaymentOperation.Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.NOT_APPLICABLE,
                status = AuthPaymentOperation.Status.PENDING,
                transactionType = payment.payload.authorizationType.toTransactionType()
            )

        }?.let { operations.add(it) }
    }

    private fun addOperation(payment: AuthorizePending, operations: MutableList<AuthPaymentOperation>)
    {
        when (payment)
        {
            is ReadyForClientActionResponse -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.threeDSStatus.toECI(),
                exemption = payment.threeDSStatus.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.PENDING,
                status = AuthPaymentOperation.Status.PENDING,
                transactionType = payment.payload.authorizationType.toTransactionType()
            )

        }.let { operations.add(it) }
    }

    private fun addOperation(payment: AuthorizeEnded, operations: MutableList<AuthPaymentOperation>)
    {
        when (payment)
        {
            is RejectedByGatewayAndNotRetriable -> null

            is Authorized -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.threeDSStatus.toECI(),
                exemption = payment.threeDSStatus.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.OK,
                transactionType = payment.payload.authorizationType.toTransactionType()
            )
            is Failed -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference =payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.threeDSStatus.toECI(),
                exemption = payment.threeDSStatus.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO,
                transactionType = payment.payload.authorizationType.toTransactionType()
            )
            is RejectedByGateway -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference =payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.threeDSStatus.toECI(),
                exemption = payment.threeDSStatus.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO,
                transactionType = payment.payload.authorizationType.toTransactionType()
            )
            is RejectedByRisk -> AuthPaymentOperation(
                paymentAccount = null,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = AuthPaymentOperation.Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO,
                transactionType = payment.payload.authorizationType.toTransactionType()
            )
            is RejectedByRouting -> AuthPaymentOperation(
                paymentAccount = null,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = AuthPaymentOperation.Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO,
                transactionType = payment.payload.authorizationType.toTransactionType()
            )
            is RejectedByRoutingRetry -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = AuthPaymentOperation.Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO,
                transactionType = payment.payload.authorizationType.toTransactionType()
            )

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
            null -> AuthPaymentOperation.Exemption.NotRequested
            is ThreeDSStatus.NoThreeDS -> AuthPaymentOperation.Exemption.NotRequested
            is ThreeDSStatus.PendingThreeDS -> AuthPaymentOperation.Exemption.NotRequested
            is ThreeDSStatus.ThreeDS -> when (this.info.exemptionStatus)
            {
                is ExemptionStatus.ExemptionNotRequested -> AuthPaymentOperation.Exemption.NotRequested
                is ExemptionStatus.ExemptionAccepted -> AuthPaymentOperation.Exemption.Accepted
                is ExemptionStatus.ExemptionNotAccepted -> AuthPaymentOperation.Exemption.NotAccepted
            }
        }

    private fun ThreeDSStatus?.toECI() =

        when (this)
        {
            null -> null
            is ThreeDSStatus.NoThreeDS -> null
            is ThreeDSStatus.PendingThreeDS -> null
            is ThreeDSStatus.ThreeDS -> this.info.eci.value.toString()
        }

    private fun AuthorizationType.toTransactionType() =

        when (this)
        {
            AuthorizationType.FULL_AUTHORIZATION -> TransactionType.AUTHORIZE_AND_SETTLE
            AuthorizationType.PRE_AUTHORIZATION -> TransactionType.AUTHORIZE
        }

    private fun print(currentState: Payment) =
        println("${if (currentState is AuthorizeEnded) "END: " else ""}${currentState::class.java.simpleName}")
}
