package application

import domain.authorize.steps.fraud.FraudAnalysisResult
import domain.authorize.steps.fraud.RiskAssessmentOutcome
import domain.authorize.steps.fraud.RiskAssessmentService
import domain.authorize.steps.gateway.*
import domain.authorize.steps.routing.PaymentAccount
import domain.authorize.steps.routing.RoutingResult
import domain.authorize.steps.routing.RoutingService
import domain.payment.PaymentPayload
import domain.payment.payload.AuthorizationReference
import domain.payment.payload.Customer
import domain.payment.payload.PaymentId
import domain.payment.payload.paymentmethod.CreditCardPayment
import infrastructure.EventPublisherMemory
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
    private val eventPublisher = EventPublisherMemory()
    private val paymentRepository = PaymentRepositoryMemory()

    private val underTest = AuthorizeUseCase(
        riskService = riskService,
        routingService = routingService,
        authorizeService = authorizationGateway,
        eventPublisher = eventPublisher,
        paymentRepository = paymentRepository
    )

    @Test
    fun no3ds()
    {
        val paymentId = PaymentId(UUID.randomUUID())
        val paymentPayload = PaymentPayload(
            paymentId = paymentId,
            authorizationReference = AuthorizationReference(id = "123456789"),
            customer = Customer("ivan", "delgado"),
            paymentMethod = CreditCardPayment
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

        underTest.authorize(paymentPayload)

        println("\nPAYMENT EVENTS:\n")
        paymentRepository.loadEvents(paymentId) .forEach { println(it) }
        println("\nSIDE EFFECTS:\n")
        eventPublisher.list.forEach { println(it) }
    }

    @Test
    fun threeDS()
    {
        val paymentId = PaymentId(UUID.randomUUID())
        val paymentPayload = PaymentPayload(
            paymentId = paymentId,
            authorizationReference = AuthorizationReference(id = "123456789"),
            customer = Customer("ivan", "delgado"),
            paymentMethod = CreditCardPayment
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

        underTest.authorize(paymentPayload)
        underTest.confirm(paymentId, mapOf("ECI" to "05"))

        println("\nPAYMENT EVENTS:\n")
        paymentRepository.loadEvents(paymentId) .forEach { println(it) }
        println("\nSIDE EFFECTS:\n")
        eventPublisher.list.forEach { println(it) }
    }
}
