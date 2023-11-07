package infrastructure.repositories.paymentrepositoryold

import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.paymentpayload.AuthorizationType
import domain.payment.data.threedstatus.ExemptionStatus
import domain.payment.data.threedstatus.ThreeDSStatus
import domain.payment.paymentevents.PaymentEvent
import domain.payment.paymentevents.RiskEvaluatedEvent
import domain.payment.state.*
import domain.services.fraud.FraudAnalysisResult
import domain.services.gateway.AuthenticateOutcome
import domain.services.gateway.AuthenticateResponse
import domain.services.gateway.AuthorizeOutcome
import domain.services.gateway.AuthorizeResponse
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
            is ReadyForAuthenticationContinuation -> null
            is ReadyForAuthenticationAndAuthorizeContinuation -> null
            is ReadyForRoutingRetry -> null
            is ReadyForCaptureVerification -> null
            is ReadyToDecideAuthMethod -> null
            is ReadyForECIVerfication -> null

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

            is ReadyForAuthenticationAndAuthorization -> if (isLastEvent) AuthPaymentOperation(
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
                pspReference = payment.authenticateOutcome.pspReference.value,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateOutcome.threeDSStatus.toECI(),
                exemption = payment.authenticateOutcome.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.PENDING,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.PENDING,
                paymentClassName = payment.toPaymentClassName()
            ) else null

            is ReadyForAuthenticationAndAuthorizeClientAction -> if (isLastEvent) AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = payment.authenticateOutcome.pspReference.value,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateOutcome.threeDSStatus.toECI(),
                exemption = payment.authenticateOutcome.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.PENDING,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.PENDING,
                paymentClassName = payment.toPaymentClassName()
            ) else null

            // FINAL STATES:
            //----------------------------------------------------------------------------------------------------------

            is RejectedByGatewayAndNotRetriable -> null
            is RejectedByRoutingSameAccount -> null

            is Authorized -> if (payment.payload.authorizationType == AuthorizationType.PRE_AUTHORIZATION)
                AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = toPSPReference(payment.authenticateOutcome, payment.authorizeOutcome),
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateOutcome.toECI(),
                exemption = toExemption(payment.authenticateOutcome, payment.authorizeOutcome),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.OK,
                paymentClassName = payment.toPaymentClassName()
            ) else null

            is Captured -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = toPSPReference(payment.authenticateOutcome, payment.authorizeOutcome),
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateOutcome.toECI(),
                exemption = toExemption(payment.authenticateOutcome, payment.authorizeOutcome),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.OK,
                paymentClassName = payment.toPaymentClassName()
            )

            is Failed -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = toPSPReference(payment.authenticateOutcome, payment.authorizeOutcome),
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateOutcome?.toECI(),
                exemption = toExemption(payment.authenticateOutcome, payment.authorizeOutcome),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.KO,
                paymentClassName = payment.toPaymentClassName()
            )

            is RejectedByAuthorization -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = payment.authorizeOutcome.pspReference.value,
                reference =payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateOutcome.toECI(),
                exemption = toExemption(payment.authenticateOutcome, payment.authorizeOutcome),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.KO,
                paymentClassName = payment.toPaymentClassName()
            )

            is RejectedByAuthentication -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = payment.authenticateOutcome.pspReference.value,
                reference =payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateOutcome.threeDSStatus.toECI(),
                exemption = payment.authenticateOutcome.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.COMPLETED,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.KO,
                paymentClassName = payment.toPaymentClassName()
            )

            is RejectedByECIVerification -> AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = payment.authenticateOutcome.pspReference.value,
                reference =payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateOutcome.threeDSStatus.toECI(),
                exemption = payment.authenticateOutcome.toExemption(),
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


    private fun toPSPReference(authenticateOutcome: AuthenticateOutcome?, authorizeOutcome: AuthorizeOutcome?): String =

        when (authenticateOutcome)
        {
            is AuthenticateResponse -> authenticateOutcome.pspReference.value
            null, is AuthenticateOutcome.Skipped -> when (authorizeOutcome)
            {
                is AuthorizeResponse -> authorizeOutcome.pspReference.value
                null, is AuthorizeOutcome.Skipped -> "SKIPED"
            }
        }

    private fun toExemption(authenticateOutcome: AuthenticateOutcome?, authorizeOutcome: AuthorizeOutcome?): Exemption =

        when (authenticateOutcome.toExemption())
        {
            Exemption.Accepted -> Exemption.Accepted
            Exemption.NotAccepted -> Exemption.NotAccepted
            Exemption.NotRequested -> authorizeOutcome.toExemption()
        }

    private fun AuthenticateOutcome?.toExemption(): Exemption =

        when (this)
        {
            is AuthenticateResponse -> this.exemptionStatus.toExemption()
            null, AuthenticateOutcome.Skipped -> Exemption.NotRequested
        }

    private fun AuthorizeOutcome?.toExemption(): Exemption =

        when (this)
        {
            is AuthorizeResponse -> this.exemptionStatus.toExemption()
            null, AuthorizeOutcome.Skipped -> Exemption.NotRequested
        }

    private fun ExemptionStatus.toExemption(): Exemption =

        when (this)
        {
            is ExemptionStatus.ExemptionNotRequested -> Exemption.NotRequested
            is ExemptionStatus.ExemptionAccepted -> Exemption.Accepted
            is ExemptionStatus.ExemptionNotAccepted -> Exemption.NotAccepted
        }

    private fun AuthenticateOutcome?.toECI() =

        when (this)
        {
            is AuthenticateResponse -> this.threeDSStatus.toECI()
            null, AuthenticateOutcome.Skipped -> null
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
