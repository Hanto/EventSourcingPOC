package application

import domain.payment.data.PSPReference
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.paymentaccount.AccountId
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.*
import domain.payment.data.paymentpayload.paymentmethod.CreditCardPayment
import domain.payment.data.threedstatus.*
import domain.services.fraud.FraudAnalysisResult
import domain.services.fraud.RiskAssessmentService
import domain.services.gateway.*
import domain.services.gateway.AuthorizeResponse.*
import domain.services.routing.RoutingResult
import domain.services.routing.RoutingService
import infrastructure.EventPublisherMemory
import infrastructure.repositories.paymentrepositorynew.PaymentRepositoryInMemory
import infrastructure.repositories.paymentrepositoryold.PaymentAdapter
import infrastructure.repositories.paymentrepositoryold.PaymentRepositoryLegacyInMemory
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class AuthorizeUseCaseTest
{
    private val riskService = mockk<RiskAssessmentService>()
    private val routingService = mockk<RoutingService>()
    private val authorizationGateway = mockk<AuthorizationGateway>()
    private val eventPublisher = EventPublisherMemory()
    private val paymentRepositoryNew = PaymentRepositoryInMemory()
    private val paymentRepositoryOld = PaymentRepositoryLegacyInMemory(paymentRepositoryNew, PaymentAdapter())

    private val underTest = AuthorizeUseCase(
        riskService = riskService,
        routingService = routingService,
        authorizeService = authorizationGateway,
        eventPublisher = eventPublisher,
        paymentRepository = paymentRepositoryNew,
        paymentRepositoryLegacy = paymentRepositoryOld,
    )

    private val paymentId = PaymentId(UUID.randomUUID())
    private val paymentPayload = PaymentPayload(
        id = paymentId,
        authorizationReference = AuthorizationReference(value = "123456789_1"),
        customer = Customer("ivan", "delgado"),
        paymentMethod = CreditCardPayment,
        authorizationType = AuthorizationType.PRE_AUTHORIZATION)

    @Nested
    inner class No3DS
    {
        @Test
        fun whenNoRetry()
        {
            val threeDSStatus = ThreeDSStatus.NoThreeDS

            val authReject = Reject(threeDSStatus, PSPReference("pspReference"),"errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED)
            val authSuccess = Success(threeDSStatus, PSPReference("pspReference"))

            every { riskService.assessRisk(any()) }
                .returns( FraudAnalysisResult.Approved(RiskAssessmentOutcome.FRICTIONLESS) )

            every { routingService.routeForPayment(any()) }
                .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )

            every { authorizationGateway.authorize(any()) }
                .returns( authSuccess)

            underTest.authorize(paymentPayload)

            printPaymentInfo(paymentId)
        }

        @Nested
        inner class WhenRetry
        {
            @Nested
            inner class WhenFirstRetry
            {
                @Test
                fun whenRetryButSameAccount()
                {
                    val threeDSStatus = ThreeDSStatus.NoThreeDS

                    val authReject = Reject(threeDSStatus, PSPReference("pspReference"),"errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED)
                    val authSuccess = Success(threeDSStatus, PSPReference("pspReference"))

                    every { riskService.assessRisk(any()) }
                        .returns( FraudAnalysisResult.Approved(RiskAssessmentOutcome.FRICTIONLESS) )

                    every { routingService.routeForPayment(any()) }
                        .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )
                        .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )

                    every { authorizationGateway.authorize(any()) }
                        .returns( authReject)
                        .andThen( authSuccess )

                    underTest.authorize(paymentPayload)

                    printPaymentInfo(paymentId)
                }

                @Test
                fun retryAndDifferentAccount()
                {
                    val threeDSStatus = ThreeDSStatus.NoThreeDS

                    val authReject = Reject(threeDSStatus, PSPReference("pspReference"),"errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED)
                    val authSuccess = Success(threeDSStatus, PSPReference("pspReference"))

                    every { riskService.assessRisk(any()) }
                        .returns( FraudAnalysisResult.Approved(RiskAssessmentOutcome.FRICTIONLESS) )

                    every { routingService.routeForPayment(any()) }
                        .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )
                        .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id2"))) )

                    every { authorizationGateway.authorize(any()) }
                        .returns( authReject)
                        .andThen( authSuccess )

                    underTest.authorize(paymentPayload)

                    printPaymentInfo(paymentId)
                }
            }

            @Test
            fun whenSecondRetry()
            {
                val threeDSStatus = ThreeDSStatus.NoThreeDS

                val authReject = Reject(threeDSStatus, PSPReference("pspReference"),"errorDescription", "errorCode", ErrorReason.AUTHORIZATION_ERROR, RejectionUseCase.UNDEFINED)
                val authSuccess = Success(threeDSStatus, PSPReference("pspReference"))

                every { riskService.assessRisk(any()) }
                    .returns( FraudAnalysisResult.Approved(RiskAssessmentOutcome.FRICTIONLESS) )

                every { routingService.routeForPayment(any()) }
                    .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )
                    .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id2"))) )
                    .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id3"))) )

                every { authorizationGateway.authorize(any()) }
                    .returns( authReject)
                    .andThen( authReject )
                    .andThen( authSuccess )

                underTest.authorize(paymentPayload)

                printPaymentInfo(paymentId)
            }
        }
    }

    @Nested
    inner class When3DS
    {
        @Test
        fun when3DSPending()
        {
            val threeDSStatus =  ThreeDSStatus.PendingThreeDS

            val authClientAction = ClientActionRequested(threeDSStatus, PSPReference("pspReference"), ClientAction(ActionType.CHALLENGE))

            every { riskService.assessRisk(any()) }
                .returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.AUTHENTICATION_MANDATORY) )

            every { routingService.routeForPayment(any()) }
                .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )

            every { authorizationGateway.authorize(any()) }
                .returns( authClientAction )

            underTest.authorize(paymentPayload)

            printPaymentInfo(paymentId)
        }

        @Nested
        inner class When3DSCompleted
        {
            @Nested
            inner class WhenRetry
            {
                @Nested
                inner class WhenFirstRetry
                {
                    @Test
                    fun whenRetryButSameAccount()
                    {

                    }

                    @Test
                    fun whenRetryAndDifferentAccount()
                    {

                        val authClientAction = ClientActionRequested(
                            threeDSStatus = ThreeDSStatus.PendingThreeDS,
                            pspReference = PSPReference("pspReference"),
                            clientAction = ClientAction(ActionType.CHALLENGE)
                        )
                        val authReject = Reject(
                            threeDSStatus = ThreeDSStatus.ThreeDS(ThreeDSInformation(
                                exemptionStatus = ExemptionStatus.ExemptionNotAccepted,
                                version = ThreeDSVersion("2.1"),
                                eci = ECI(5))),
                            pspReference = PSPReference("pspReference"),
                            errorDescription = "errorDescription",
                            errorCode = "errorCode",
                            errorReason = ErrorReason.AUTHORIZATION_ERROR,
                            rejectionUseCase =  RejectionUseCase.UNDEFINED
                        )
                        val authSuccess = Success(
                            threeDSStatus = ThreeDSStatus.ThreeDS(ThreeDSInformation(
                                exemptionStatus = ExemptionStatus.ExemptionNotAccepted,
                                version = ThreeDSVersion("2.1"),
                                eci = ECI(2))),
                            pspReference = PSPReference("pspReference")
                        )

                        every { riskService.assessRisk(any()) }
                            .returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.AUTHENTICATION_MANDATORY) )

                        every { routingService.routeForPayment(any()) }
                            .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )
                            .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id2"))) )
                            .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id3"))) )

                        every { authorizationGateway.authorize(any()) }
                            .returns( authClientAction )
                            .andThen( authSuccess )

                        every { authorizationGateway.confirm( any()) }
                            .returns( authReject )

                        underTest.authorize(paymentPayload)
                        underTest.confirm(paymentId, mapOf("ECI" to "05"))

                        printPaymentInfo(paymentId)
                    }
                }

                @Test
                fun whenSecondRetry()
                {

                }
            }
        }
    }

    // HELPER:
    //------------------------------------------------------------------------------------------------------------------

    private fun printPaymentInfo(paymentId: PaymentId)
    {
        println("\nPAYMENT EVENTS:\n")
        paymentRepositoryNew.loadEvents(paymentId).forEach { println(it) }

        println("\nSIDE EFFECTS:\n")
        eventPublisher.list.forEach { println(it) }

        println("\nPAYMENT DATA:\n")
        val paymentData = paymentRepositoryOld.loadPaymentData(paymentId)
        println(paymentData)

        println("\nPAYMENT OPERATIONS:\n")
        paymentData?.operations?.forEach { println(it) }
        println("\n")
    }
}
