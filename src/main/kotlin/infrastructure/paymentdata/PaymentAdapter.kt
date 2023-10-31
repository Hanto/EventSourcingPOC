package infrastructure.paymentdata

import domain.payment.lifecycle.events.PaymentEvent
import domain.payment.lifecycle.events.RiskEvaluatedEvent
import domain.payment.lifecycle.status.*
import domain.services.fraud.FraudAnalysisResult
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.gateway.ExemptionStatus
import domain.services.gateway.ThreeDSStatus

class PaymentAdapter
{
    fun toPaymentData(events: List<PaymentEvent>): PaymentData
    {
        return PaymentData(
            operations = retrieveOperations(events),
            riskAssessmentOutcome = retrieveRiskAssessment(events)
        )
    }

    // FIELDS:
    //------------------------------------------------------------------------------------------------------------------

    private fun retrieveRiskAssessment(events: List<PaymentEvent>): RiskAssessmentOutcome? =

        events
            .filterIsInstance<RiskEvaluatedEvent>()
            .map { toRiskAssessmentOutcome(it.fraudAnalysisResult) }
            .firstOrNull()

    private fun retrieveOperations(events: List<PaymentEvent>): List<AuthPaymentOperation>
    {
        val operations: MutableList<AuthPaymentOperation> = mutableListOf()
        events.foldIndexed(ReadyForPaymentRequest() as Payment) { index, previousState, event ->

            val currentState = previousState.apply(event, false)
            println("${if (currentState is AuthorizeEnded) "END: " else ""}${currentState::class.java.simpleName}")

            when (currentState)
            {
                is AuthorizeInProgress -> if (isLastEvent(index, events)) addOperation(currentState, operations)
                is AuthorizePending -> if (isLastEvent(index, events)) addOperation(currentState, operations)
                is AuthorizeEnded -> addOperation(currentState, operations)
            }
            currentState
        }
        return operations
    }

    private fun isLastEvent(index: Int, events: List<PaymentEvent>) = index == events.size - 1

    // OPERATIONS:
    //------------------------------------------------------------------------------------------------------------------

    private fun addOperation(payment: AuthorizeInProgress, operations: MutableList<AuthPaymentOperation>)
    {
        val operation = when (payment)
        {
            is ReadyForPaymentRequest -> null
            is ReadyForRisk -> null
            is ReadyForRoutingInitial -> null
            is ReadyForAuthorization -> null
            is ReadyForConfirm -> null
            is ReadyForRoutingRetry -> null

        }?.let { operations.add(it) }
    }

    private fun addOperation(payment: AuthorizePending, operations: MutableList<AuthPaymentOperation>)
    {
        val operation = when (payment)
        {
            is ReadyForClientActionResponse -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = toECI(payment.threeDSStatus),
                exemption = toExemption(payment.threeDSStatus),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.PENDING,
                status = AuthPaymentOperation.Status.PENDING
            )

        }.let { operations.add(it) }
    }

    private fun addOperation(payment: AuthorizeEnded, operations: MutableList<AuthPaymentOperation>)
    {
        val operation = when (payment)
        {
            is RejectedByGatewayAndNotRetriable -> null

            is Authorized -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = toECI(payment.threeDSStatus),
                exemption = toExemption(payment.threeDSStatus),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.OK
            )
            is Failed -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference =payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = toECI(payment.threeDSStatus),
                exemption = toExemption(payment.threeDSStatus),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO
            )
            is RejectedByGateway -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference =payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = toECI(payment.threeDSStatus),
                exemption = toExemption(payment.threeDSStatus),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO
            )
            is RejectedByRisk -> AuthPaymentOperation(
                paymentAccount = null,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = AuthPaymentOperation.Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO
            )
            is RejectedByRouting -> AuthPaymentOperation(
                paymentAccount = null,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = AuthPaymentOperation.Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO
            )
            is RejectedByRoutingRetry -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = AuthPaymentOperation.Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO
            )

        }?.let { operations.add(it) }
    }

    // ADAPTERS:
    //------------------------------------------------------------------------------------------------------------------

    private fun toRiskAssessmentOutcome(it: FraudAnalysisResult) =

        when (it)
        {
            is FraudAnalysisResult.Approved -> it.riskAssessmentOutcome
            FraudAnalysisResult.Denied -> null
        }

    private fun toExemption(threeDSStatus: ThreeDSStatus?) =

        when (threeDSStatus)
        {
            null -> AuthPaymentOperation.Exemption.NotRequested
            is ThreeDSStatus.NoThreeDS -> AuthPaymentOperation.Exemption.NotRequested
            is ThreeDSStatus.PendingThreeDS -> AuthPaymentOperation.Exemption.NotRequested
            is ThreeDSStatus.ThreeDS -> when (threeDSStatus.info.exemptionStatus)
            {
                is ExemptionStatus.ExemptionNotRequested -> AuthPaymentOperation.Exemption.NotRequested
                is ExemptionStatus.ExemptionAccepted -> AuthPaymentOperation.Exemption.Accepted
                is ExemptionStatus.ExemptionNotAccepted -> AuthPaymentOperation.Exemption.NotAccepted
            }
        }

    private fun toECI(threeDSStatus: ThreeDSStatus?) =

        when (threeDSStatus)
        {
            null -> null
            is ThreeDSStatus.NoThreeDS -> null
            is ThreeDSStatus.PendingThreeDS -> null
            is ThreeDSStatus.ThreeDS -> threeDSStatus.info.eci.value.toString()
        }
}
