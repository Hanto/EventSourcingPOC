package infrastructure.repositories.paymentrepositoryold

import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.Version
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.PaymentId
import domain.payment.paymentevents.AuthorizationRequestedEvent
import domain.payment.paymentevents.PaymentEvent
import domain.payment.paymentevents.RiskEvaluatedEvent
import domain.payment.paymentevents.RoutingEvaluatedEvent
import domain.services.fraud.FraudAnalysisResult
import domain.services.gateway.AuthorizeResponse
import domain.services.routing.RoutingResult

class PaymentDataAdapter
{
    fun toPayment(paymentData: PaymentData): List<PaymentEvent>
    {
        // add Payload


        TODO()
    }

    private fun toRiskEvaluatedEvent(paymentData: PaymentData) =

        RiskEvaluatedEvent(
            paymentId = PaymentId(paymentData.id),
            version = Version(1),
            fraudAnalysisResult = toFraudAnalysisResult(paymentData.riskAssessmentOutcome))

    private fun toFirstRoutingEvaluatedEvent(paymentData: PaymentData, firstOperation: AuthPaymentOperation) =

        RoutingEvaluatedEvent(
            paymentId = PaymentId(paymentData.id),
            version = Version(2),
            routingResult = toRoutingResult(firstOperation.paymentAccount)
        )

    private fun toFirstAuthorizationRequestedEvent(paymentData: PaymentData, firstOperation: AuthPaymentOperation) =

        AuthorizationRequestedEvent(
            paymentId = PaymentId(paymentData.id),
            version = Version(3),
            authorizeResponse = toAuthorizeResponse(firstOperation)
        )

    private fun toFraudAnalysisResult(riskAssessmentOutcome: RiskAssessmentOutcome?) =

        when(riskAssessmentOutcome)
        {
            null -> FraudAnalysisResult.Denied
            else -> FraudAnalysisResult.Approved(riskAssessmentOutcome)
        }

    private fun toRoutingResult(paymentAccount: PaymentAccount?) =

        when (paymentAccount)
        {
            null -> RoutingResult.Reject
            else -> RoutingResult.Proceed(paymentAccount)
        }

    private fun toAuthorizeResponse(operation: AuthPaymentOperation): AuthorizeResponse
    {
        TODO()
    }

}
