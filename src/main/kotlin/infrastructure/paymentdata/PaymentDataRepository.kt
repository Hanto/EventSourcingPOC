package infrastructure.paymentdata

import domain.payment.lifecycle.events.PaymentEvent
import domain.payment.lifecycle.events.RiskEvaluatedEvent
import domain.payment.lifecycle.status.*
import domain.repositories.PaymentRepository
import domain.services.fraud.FraudAnalysisResult
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.gateway.ExemptionStatus
import domain.services.gateway.ThreeDSStatus

class PaymentDataRepository
(
    private val paymentRepository: PaymentRepository
)
{
    fun save(input: Payment): PaymentData
    {
        val events = paymentRepository.loadEvents(input.payload().paymentId)

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
                is AuthorizeInProgress -> { if (index == events.size -1) addOperation(currentState, operations) }
                is AuthorizePending -> { if (index == events.size -1) addOperation(currentState, operations) }
                is AuthorizeEnded -> addOperation(currentState, operations)
            }
            currentState
        }
        return operations
    }

    // OPERATIONS:
    //------------------------------------------------------------------------------------------------------------------

    private fun addOperation(payment: AuthorizeInProgress, operations: MutableList<AuthPaymentOperation>)
    {
        val operation = when (payment)
        {
            is ReadyForAuthorization -> TODO()
            is ReadyForConfirm -> TODO()
            is ReadyForPaymentRequest -> TODO()
            is ReadyForRisk -> TODO()
            is ReadyForRoutingInitial -> TODO()
            is ReadyForRoutingRetry -> TODO()
        }
    }

    private fun addOperation(payment: AuthorizePending, operations: MutableList<AuthPaymentOperation>)
    {
        val operation = when (payment)
        {
            is ReadyForClientActionResponse -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference = "todo",
                retry = payment.attempt.didRetry(),
                eci = toECI(payment.threeDSStatus),
                exemption = toExemption(payment.threeDSStatus),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.PENDING,
                status = AuthPaymentOperation.Status.PENDING
            )
        }
        operations.add(operation)
    }

    private fun addOperation(payment: AuthorizeEnded, operations: MutableList<AuthPaymentOperation>)
    {
        val operation = when (payment)
        {
            is Authorized -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference = "todo",
                retry = payment.attempt.didRetry(),
                eci = toECI(payment.threeDSStatus),
                exemption = toExemption(payment.threeDSStatus),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.OK
            )
            is Failed -> TODO()

            is RejectedByGateway -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference = "todo",
                retry = payment.attempt.didRetry(),
                eci = toECI(payment.threeDSStatus),
                exemption = toExemption(payment.threeDSStatus),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO
            )
            is RejectedByGatewayAndNotRetriable -> null

            is RejectedByRisk -> AuthPaymentOperation(
                paymentAccount = null,
                reference = "todo",
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = AuthPaymentOperation.Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO
            )
            is RejectedByRouting -> AuthPaymentOperation(
                paymentAccount = null,
                reference = "todo",
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = AuthPaymentOperation.Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO
            )
            is RejectedByRoutingRetry -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                reference = "todo",
                retry = payment.attempt.didRetry(),
                eci = null,
                exemption = AuthPaymentOperation.Exemption.NotRequested,
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                status = AuthPaymentOperation.Status.KO
            )
        }
        operation?.let { operations.add(it) }
    }

    // ADAPTERS:
    //------------------------------------------------------------------------------------------------------------------

    private fun toRiskAssessmentOutcome(it: FraudAnalysisResult) =

        when (it)
        {
            is FraudAnalysisResult.Approved -> it.riskAssessmentOutcome
            FraudAnalysisResult.Denied -> null
        }

    private fun toExemption(threeDSStatus: ThreeDSStatus) =

        when (threeDSStatus)
        {
            is ThreeDSStatus.NoThreeDS -> AuthPaymentOperation.Exemption.NotRequested
            is ThreeDSStatus.PendingThreeDS -> AuthPaymentOperation.Exemption.NotRequested
            is ThreeDSStatus.ThreeDS -> when (threeDSStatus.info.exemptionStatus)
            {
                is ExemptionStatus.ExemptionNotRequested -> AuthPaymentOperation.Exemption.NotRequested
                is ExemptionStatus.ExemptionAccepted -> AuthPaymentOperation.Exemption.Accepted
                is ExemptionStatus.ExemptionNotAccepted -> AuthPaymentOperation.Exemption.NotAccepted
            }
        }

    private fun toECI(threeDSStatus: ThreeDSStatus) =

        when (threeDSStatus)
        {
            is ThreeDSStatus.NoThreeDS -> null
            is ThreeDSStatus.PendingThreeDS -> null
            is ThreeDSStatus.ThreeDS -> threeDSStatus.info.eci.value.toString()
        }
}
