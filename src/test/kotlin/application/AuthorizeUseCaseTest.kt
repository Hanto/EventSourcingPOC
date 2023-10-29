package application

import domain.authorize.steps.fraud.FraudAnalysisResult
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.fraud.RiskAssessmentService
import domain.authorize.steps.gateway.*
import domain.authorize.steps.routing.PaymentAccount
import domain.authorize.steps.routing.RoutingResult
import domain.authorize.steps.routing.RoutingService
import domain.payment.AuthorizationReference
import domain.payment.Customer
import domain.payment.PaymentId
import domain.payment.PaymentPayload
import infrastructure.PaymentRepositoryMemory
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.*

class AuthorizeUseCaseTest
{
    private val riskService = mockk<RiskAssessmentService>()
    private val routingService = mockk<RoutingService>()
    private val authorizationGateway = mockk<AuthorizationGateway>()
    private val paymentRepository = PaymentRepositoryMemory()

    private val underTest = AuthorizeUseCase(
        riskService = riskService,
        routingService = routingService,
        authorizeService = authorizationGateway,
        paymentRepository = paymentRepository
    )

    @Test
    fun no3ds()
    {
        val paymentPayload = PaymentPayload(
            paymentId = PaymentId(UUID.randomUUID()),
            authorizationReference = AuthorizationReference(id = "123456789"),
            customer = Customer("ivan", "delgado")
        )

        every { riskService.assessRisk(any()) }.returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.FRICTIONLESS) )
        every { routingService.routeForPayment(any()) }
            .returns( RoutingResult.Proceed(PaymentAccount()) )
            .andThen( RoutingResult.Proceed(PaymentAccount()) )
            .andThen( RoutingResult.Proceed(PaymentAccount()) )

        every { authorizationGateway.authorize(any()) }
            .returns( AuthorizeResponse(AuthorizeStatus.Reject("errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED) ))
            .andThen( AuthorizeResponse(AuthorizeStatus.Reject("errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED) ))
            .andThen( AuthorizeResponse(AuthorizeStatus.Success) )

        val payment = underTest.authorize(paymentPayload)

        println("\nPAYMENT EVENTS:\n")
        payment.getNewEvents().forEach { println(it) }
        println("\nSIDE EFFECTS:\n")
        payment.getNewSideEffects().forEach { println(it) }
    }

    @Test
    fun threeDS()
    {
        val paymentId = PaymentId(UUID.randomUUID())
        val paymentPayload = PaymentPayload(
            paymentId = paymentId,
            authorizationReference = AuthorizationReference(id = "123456789"),
            customer = Customer("ivan", "delgado")
        )

        every { riskService.assessRisk(any()) }.returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.FRICTIONLESS) )
        every { routingService.routeForPayment(any()) }
            .returns( RoutingResult.Proceed(PaymentAccount()) )
            .andThen( RoutingResult.Proceed(PaymentAccount()) )
            .andThen( RoutingResult.Proceed(PaymentAccount()) )

        every { authorizationGateway.authorize(any()) }
            .returns( AuthorizeResponse(AuthorizeStatus.Reject("errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED) ))
            .andThen( AuthorizeResponse(AuthorizeStatus.ClientActionRequested(ClientAction(ActionType.CHALLENGE)) ))
            .andThen( AuthorizeResponse(AuthorizeStatus.Success) )

        every { authorizationGateway.confirm( any()) }.returns( AuthorizeResponse(AuthorizeStatus.Reject("errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED)) )

        val paymentOnAuthorize = underTest.authorize(paymentPayload)
        val paymentOnConfirm = underTest.confirm(paymentId, emptyMap())

        println("\nPAYMENT EVENTS:\n")
        paymentOnAuthorize.getNewEvents().forEach { println(it) }
        paymentOnConfirm.getNewEvents().forEach { println(it) }
        println("\nSIDE EFFECTS:\n")
        paymentOnAuthorize.getNewSideEffects().forEach { println(it) }
        paymentOnConfirm.getNewSideEffects().forEach { println(it) }
    }

}
