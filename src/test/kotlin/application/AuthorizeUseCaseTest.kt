package application

import domain.payment.payload.*
import domain.payment.payload.paymentmethod.CreditCardPayment
import domain.services.fraud.FraudAnalysisResult
import domain.services.fraud.RiskAssessmentOutcome
import domain.services.fraud.RiskAssessmentService
import domain.services.gateway.*
import domain.services.routing.AccountId
import domain.services.routing.PaymentAccount
import domain.services.routing.RoutingResult
import domain.services.routing.RoutingService
import infrastructure.EventPublisherMemory
import infrastructure.PaymentRepositoryMemory
import infrastructure.paymentdata.PaymentAdapter
import infrastructure.paymentdata.PaymentDataRepository
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
    private val paymentDataRepository = PaymentDataRepository(paymentRepository, PaymentAdapter())

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
            authorizationReference = AuthorizationReference(value = "123456789_1"),
            customer = Customer("ivan", "delgado"),
            paymentMethod = CreditCardPayment,
            authorizationType = AuthorizationType.PRE_AUTHORIZATION
        )

        val threeDSStatus = ThreeDSStatus.NoThreeDS

        val authReject = AuthorizeResponse.Reject(threeDSStatus, PSPReference("pspReference"),"errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED)
        val authSuccess = AuthorizeResponse.Success(threeDSStatus, PSPReference("pspReference"))

        every { riskService.assessRisk(any()) }
            .returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.FRICTIONLESS) )

        every { routingService.routeForPayment(any()) }
            .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )
            .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )
            .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id3"))) )

        every { authorizationGateway.authorize(any()) }
            .returns( authReject)
            .andThen( authReject )
            .andThen( authSuccess )

        val payment = underTest.authorize(paymentPayload)

        println("\nPAYMENT EVENTS:\n")
        paymentRepository.loadEvents(paymentId) .forEach { println(it) }
        println("\nSIDE EFFECTS:\n")
        eventPublisher.list.forEach { println(it) }
        println("\nPAYMENT DATA:\n")
        val paymentData = paymentDataRepository.save(payment)
        println("\nPAYMENT OPERATIONS:\n")
        println(paymentData)
        paymentData.operations.forEach { println(it) }
    }

    @Test
    fun threeDSPending()
    {
        val paymentId = PaymentId(UUID.randomUUID())
        val paymentPayload = PaymentPayload(
            paymentId = paymentId,
            authorizationReference = AuthorizationReference(value = "123456789_1"),
            customer = Customer("ivan", "delgado"),
            paymentMethod = CreditCardPayment,
            authorizationType = AuthorizationType.PRE_AUTHORIZATION
        )
        val threeDSInformation = ThreeDSInformation(
            exemptionStatus = ExemptionStatus.ExemptionNotRequested,
            version = ThreeDSVersion("2.1"),
            eci = ECI(5)
        )

        val authReject = AuthorizeResponse.Reject(ThreeDSStatus.ThreeDS(threeDSInformation), PSPReference("pspReference"),"errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED)
        val authClientAction = AuthorizeResponse.ClientActionRequested(ThreeDSStatus.PendingThreeDS, PSPReference("pspReference"), ClientAction(ActionType.CHALLENGE))
        val authSuccess = AuthorizeResponse.Success(ThreeDSStatus.ThreeDS(threeDSInformation), PSPReference("pspReference"), )

        every { riskService.assessRisk(any()) }
            .returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.AUTHENTICATION_MANDATORY) )

        every { routingService.routeForPayment(any()) }
            .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )
            .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id2"))) )
            .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id3"))) )

        every { authorizationGateway.authorize(any()) }
            .returns( authClientAction )
            .andThen( authSuccess )
            .andThen( authSuccess )

        val payment = underTest.authorize(paymentPayload)

        println("\nPAYMENT EVENTS:\n")
        paymentRepository.loadEvents(paymentId) .forEach { println(it) }
        println("\nSIDE EFFECTS:\n")
        eventPublisher.list.forEach { println(it) }
        println("\nPAYMENT DATA:\n")
        val paymentData = paymentDataRepository.save(payment)
        println("\nPAYMENT OPERATIONS:\n")
        println(paymentData)
        paymentData.operations.forEach { println(it) }
    }

    @Test
    fun threeDSCompleted()
    {
        val paymentId = PaymentId(UUID.randomUUID())
        val paymentPayload = PaymentPayload(
            paymentId = paymentId,
            authorizationReference = AuthorizationReference(value = "123456789_1"),
            customer = Customer("ivan", "delgado"),
            paymentMethod = CreditCardPayment,
            authorizationType = AuthorizationType.FULL_AUTHORIZATION
        )
        val threeDSInformation = ThreeDSInformation(
            exemptionStatus = ExemptionStatus.ExemptionNotRequested,
            version = ThreeDSVersion("2.1"),
            eci = ECI(5)
        )

        val authReject = AuthorizeResponse.Reject(ThreeDSStatus.ThreeDS(threeDSInformation), PSPReference("pspReference"), "errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED)
        val authClientAction = AuthorizeResponse.ClientActionRequested(ThreeDSStatus.PendingThreeDS, PSPReference("pspReference"), ClientAction(ActionType.CHALLENGE))
        val authSuccess = AuthorizeResponse.Success(ThreeDSStatus.ThreeDS(threeDSInformation), PSPReference("pspReference"), )

        every { riskService.assessRisk(any()) }
            .returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.AUTHENTICATION_MANDATORY) )

        every { routingService.routeForPayment(any()) }
            .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )
            .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id2"))) )
            .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id3"))) )

        every { authorizationGateway.authorize(any()) }
            .returns( authClientAction )
            .andThen( authSuccess )
            .andThen( authSuccess )

        every { authorizationGateway.confirm( any()) }.returns( authReject )

        underTest.authorize(paymentPayload)
        val payment = underTest.confirm(paymentId, mapOf("ECI" to "05"))

        println("\nPAYMENT EVENTS:\n")
        paymentRepository.loadEvents(paymentId) .forEach { println(it) }
        println("\nSIDE EFFECTS:\n")
        eventPublisher.list.forEach { println(it) }
        println("\nPAYMENT DATA:\n")
        val paymentData = paymentDataRepository.save(payment)
        println("\nPAYMENT OPERATIONS:\n")
        println(paymentData)
        paymentData.operations.forEach { println(it) }
    }
}
