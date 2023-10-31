package application

import domain.payment.PaymentPayload
import domain.payment.payload.AuthorizationReference
import domain.payment.payload.Customer
import domain.payment.payload.PaymentId
import domain.payment.payload.paymentmethod.CreditCardPayment
import domain.services.fraud.FraudAnalysisResult
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.fraud.RiskAssessmentService
import domain.services.gateway.*
import domain.services.routing.PaymentAccount
import domain.services.routing.RoutingResult
import domain.services.routing.RoutingService
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

        val threeDSInformation = ThreeDSInformation(
            exemptionStatus = ExemptionStatus.ExemptionNotRequested,
            version = ThreeDSVersion("2.1"),
            eci = ECI(5)
        )

        val authReject = AuthorizeResponse.Reject(ThreeDSStatus.ThreeDS(threeDSInformation),"errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED)
        val authClientAction = AuthorizeResponse.ClientActionRequested(ThreeDSStatus.PendingThreeDS, ClientAction(ActionType.CHALLENGE))
        val authSuccess = AuthorizeResponse.Success(ThreeDSStatus.ThreeDS(threeDSInformation))

        every { riskService.assessRisk(any()) }.returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.FRICTIONLESS) )
        every { routingService.routeForPayment(any()) }
            .returns( RoutingResult.Proceed(PaymentAccount()) )
            .andThen( RoutingResult.Proceed(PaymentAccount()) )
            .andThen( RoutingResult.Proceed(PaymentAccount()) )

        every { authorizationGateway.authorize(any()) }
            .returns( authReject)
            .andThen( authSuccess )
            .andThen( authSuccess )

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
        val threeDSInformation = ThreeDSInformation(
            exemptionStatus = ExemptionStatus.ExemptionNotRequested,
            version = ThreeDSVersion("2.1"),
            eci = ECI(5)
        )

        val authReject = AuthorizeResponse.Reject(ThreeDSStatus.ThreeDS(threeDSInformation),"errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED)
        val authClientAction = AuthorizeResponse.ClientActionRequested(ThreeDSStatus.PendingThreeDS, ClientAction(ActionType.CHALLENGE))
        val authSuccess = AuthorizeResponse.Success(ThreeDSStatus.ThreeDS(threeDSInformation))

        every { riskService.assessRisk(any()) }.returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.FRICTIONLESS) )
        every { routingService.routeForPayment(any()) }
            .returns( RoutingResult.Proceed(PaymentAccount()) )
            .andThen( RoutingResult.Proceed(PaymentAccount()) )
            .andThen( RoutingResult.Proceed(PaymentAccount()) )

        every { authorizationGateway.authorize(any()) }
            .returns( authClientAction )
            .andThen( authSuccess )
            .andThen( authSuccess )

        every { authorizationGateway.confirm( any()) }.returns( authReject )

        underTest.authorize(paymentPayload)
        underTest.confirm(paymentId, mapOf("ECI" to "05"))

        println("\nPAYMENT EVENTS:\n")
        paymentRepository.loadEvents(paymentId) .forEach { println(it) }
        println("\nSIDE EFFECTS:\n")
        eventPublisher.list.forEach { println(it) }
    }
}
