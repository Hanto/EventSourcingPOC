package infrastructure.repositories.paymentrepositoryold

import domain.payment.data.AuthenticateOutcome
import domain.payment.data.AuthorizeOutcome
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.paymentpayload.AuthorizationType
import domain.payment.data.threedstatus.ExemptionStatus
import domain.payment.data.threedstatus.ThreeDSStatus
import domain.payment.paymentevents.PaymentEvent
import domain.payment.paymentevents.RiskEvaluatedEvent
import domain.payment.state.*
import domain.services.fraud.FraudAnalysisResult
import domain.services.gateway.AuthenticateResponse
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
            is ReadyForAuthenticationConfirm -> null
            is ReadyForAuthenticationAndAuthorizeConfirm -> null
            is ReadyForRoutingRetry -> null
            is ReadyForCaptureVerification -> null
            is ReadyForRoutingAction -> null

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
                pspReference = payment.authenticateOutcome.authenticateResponse.pspReference.value,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateOutcome.authenticateResponse.threeDSStatus.toECI(),
                exemption = payment.authenticateOutcome.toExemption(),
                authenticationStatus = AuthPaymentOperation.AuthenticationStatus.PENDING,
                transactionType = payment.payload.authorizationType.toTransactionType(),
                status = AuthPaymentOperation.Status.PENDING,
                paymentClassName = payment.toPaymentClassName()
            ) else null

            is ReadyForAuthenticationAndAuthorizeClientAction -> if (isLastEvent) AuthPaymentOperation(
                paymentAccount = payment.paymentAccount,
                pspReference = payment.authenticateOutcome.authenticateResponse.pspReference.value,
                reference = payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateOutcome.authenticateResponse.threeDSStatus.toECI(),
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
                pspReference = payment.authorizeOutcome.authorizeResponse.pspReference.value,
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
                pspReference = payment.authenticateOutcome.authenticateResponse.pspReference.value,
                reference =payment.attemptReference().value,
                retry = payment.attempt.didRetry(),
                eci = payment.authenticateOutcome.authenticateResponse.threeDSStatus.toECI(),
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
            is AuthenticateOutcome.Performed -> authenticateOutcome.authenticateResponse.pspReference.value
            null, is AuthenticateOutcome.Skipped -> when (authorizeOutcome)
            {
                is AuthorizeOutcome.Performed -> authorizeOutcome.authorizeResponse.pspReference.value
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
            null -> Exemption.NotRequested
            is AuthenticateOutcome.Skipped -> Exemption.NotRequested
            is AuthenticateOutcome.Performed -> this.authenticateResponse.toExemption()
        }

    private fun AuthorizeOutcome?.toExemption(): Exemption =

        when (this)
        {
            null -> Exemption.NotRequested
            is AuthorizeOutcome.Skipped -> Exemption.NotRequested
            is AuthorizeOutcome.Performed -> this.authorizeResponse.toExemption()
        }

    private fun AuthenticateResponse?.toExemption(): Exemption =

        when (this?.exemptionStatus)
        {
            null -> Exemption.NotRequested
            is ExemptionStatus.ExemptionNotRequested -> Exemption.NotRequested
            is ExemptionStatus.ExemptionAccepted -> Exemption.Accepted
            is ExemptionStatus.ExemptionNotAccepted -> Exemption.NotAccepted
        }

    private fun AuthorizeResponse?.toExemption(): Exemption =

        when (this?.exemptionStatus)
        {
            null -> Exemption.NotRequested
            is ExemptionStatus.ExemptionNotRequested -> Exemption.NotRequested
            is ExemptionStatus.ExemptionAccepted -> Exemption.Accepted
            is ExemptionStatus.ExemptionNotAccepted -> Exemption.NotAccepted
        }

    private fun AuthenticateOutcome.toECI() =

        when (this)
        {
            is AuthenticateOutcome.Skipped -> null
            is AuthenticateOutcome.Performed -> this.authenticateResponse.threeDSStatus.toECI()
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
