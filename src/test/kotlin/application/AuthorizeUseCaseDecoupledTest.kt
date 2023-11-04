package application

import domain.payment.data.PSPReference
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.paymentaccount.AccountId
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.*
import domain.payment.data.paymentpayload.paymentmethod.CreditCardPayment
import domain.payment.data.threedstatus.*
import domain.services.featureflag.FeatureFlag
import domain.services.fraud.FraudAnalysisResult
import domain.services.fraud.RiskAssessmentService
import domain.services.gateway.*
import domain.services.routing.RoutingResult
import domain.services.routing.RoutingService
import infrastructure.EventPublisherMemory
import infrastructure.repositories.paymentrepositorynew.PaymentRepositoryInMemory
import infrastructure.repositories.paymentrepositoryold.PaymentAdapter
import infrastructure.repositories.paymentrepositoryold.PaymentRepositoryLegacyInMemory
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class AuthorizeUseCaseDecoupledTest
{
    private val riskService = mockk<RiskAssessmentService>()
    private val routingService = mockk<RoutingService>()
    private val authorizationGateway = mockk<AuthorizationGateway>()
    private val featureFlag = mockk<FeatureFlag>()
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
        featureFlag = featureFlag
    )

    private val paymentId = PaymentId(UUID.randomUUID())
    private val paymentPayload = PaymentPayload(
        id = paymentId,
        authorizationReference = AuthorizationReference(value = "123456789_1"),
        customer = Customer("ivan", "delgado"),
        paymentMethod = CreditCardPayment,
        authorizationType = AuthorizationType.PRE_AUTHORIZATION)

    @BeforeEach
    fun beforeEach()
    {
        every { featureFlag.isFeatureEnabledFor(FeatureFlag.Feature.DECOUPLED_AUTH) }.returns(true)
    }


    @Nested
    inner class No3DS
    {
        @Test
        fun whenNoRetry()
        {
            val threeDSStatus = ThreeDSStatus.NoThreeDS

            val authenticateSuccess = AuthenticateResponse.AuthenticateSuccess(threeDSStatus, PSPReference("pspReference"))
            val authorizeSuccess = AuthorizeResponse.AuthorizeSuccess(PSPReference("pspReference"))

            every { riskService.assessRisk(any()) }
                .returns(FraudAnalysisResult.Approved(RiskAssessmentOutcome.FRICTIONLESS))

            every { routingService.routeForPayment(any()) }
                .returns(RoutingResult.Proceed(PaymentAccount(AccountId("id1"))))

            every { authorizationGateway.authenticate(any()) }
                .returns(authenticateSuccess)

            every { authorizationGateway.authorize(any()) }
                .returns(authorizeSuccess)

            underTest.authorize(paymentPayload)

            printPaymentInfo(paymentId)
        }


        @Nested
        inner class WhenRetry
        {
            @Nested
            inner class WhenFirstRetry
            {
                @Nested
                inner class WhenDueToAuthenticationReject
                {
                    @Test
                    fun whenDifferentAccount()
                    {
                        val threeDSStatus = ThreeDSStatus.NoThreeDS

                        val authenticateSuccess = AuthenticateResponse.AuthenticateSuccess(
                            threeDSStatus = threeDSStatus,
                            pspReference = PSPReference("pspReference"),
                        )
                        val authenticateReject = AuthenticateResponse.AuthenticateReject(
                            threeDSStatus = threeDSStatus,
                            pspReference = PSPReference("pspReference"),
                            errorDescription = "errorDescription",
                            errorCode = "errorCode",
                            errorReason = ErrorReason.AUTHORIZATION_ERROR,
                            rejectionUseCase = RejectionUseCase.UNDEFINED
                        )
                        val authorizeSuccess = AuthorizeResponse.AuthorizeSuccess(
                            pspReference = PSPReference("pspReference"))

                        every { riskService.assessRisk(any()) }
                            .returns( FraudAnalysisResult.Approved(RiskAssessmentOutcome.FRICTIONLESS) )

                        every { routingService.routeForPayment(any()) }
                            .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )
                            .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id2"))) )

                        every { authorizationGateway.authenticate(any()) }
                            .returns( authenticateReject)
                            .andThen( authenticateSuccess )

                        every { authorizationGateway.authorize(any()) }
                            .returns( authorizeSuccess )

                        underTest.authorize(paymentPayload)

                        printPaymentInfo(paymentId)
                    }
                }

                @Nested
                inner class WhenDueToAuthorizationReject
                {
                    @Test
                    fun whenDifferentAccount()
                    {
                        val threeDSStatus = ThreeDSStatus.NoThreeDS

                        val authenticateSuccess = AuthenticateResponse.AuthenticateSuccess(
                            threeDSStatus = threeDSStatus,
                            pspReference = PSPReference("pspReference"),
                        )
                        val authorizeReject = AuthorizeResponse.AuthorizeReject(
                            pspReference = PSPReference("pspReference"),
                            errorDescription = "errorDescription",
                            errorCode = "errorCode",
                            errorReason = ErrorReason.AUTHORIZATION_ERROR,
                            rejectionUseCase = RejectionUseCase.UNDEFINED
                        )
                        val authorizeSuccess = AuthorizeResponse.AuthorizeSuccess(
                            pspReference = PSPReference("pspReference")
                        )

                        every { riskService.assessRisk(any()) }
                            .returns( FraudAnalysisResult.Approved(RiskAssessmentOutcome.FRICTIONLESS) )

                        every { routingService.routeForPayment(any()) }
                            .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )
                            .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id2"))) )

                        every { authorizationGateway.authenticate(any()) }
                            .returns( authenticateSuccess)
                            .andThen( authenticateSuccess )

                        every { authorizationGateway.authorize(any()) }
                            .returns( authorizeReject )
                            .andThen( authorizeSuccess )

                        underTest.authorize(paymentPayload)

                        printPaymentInfo(paymentId)
                    }
                }
            }
        }
    }

    @Nested
    inner class When3DS
    {
        @Nested
        inner class When3DSCompleted
        {
            @Nested
            inner class WhenRetry
            {
                @Test
                fun whenDifferentAccount()
                {
                    val authenticateClientActionFingerprint = AuthenticateResponse.AuthenticateClientAction(
                        threeDSStatus = ThreeDSStatus.PendingThreeDS,
                        pspReference = PSPReference("pspReference"),
                        clientAction = ClientAction(ActionType.FINGERPRINT)
                    )
                    val authenticateClientActionChallenge = AuthenticateResponse.AuthenticateClientAction(
                        threeDSStatus = ThreeDSStatus.PendingThreeDS,
                        pspReference = PSPReference("pspReference"),
                        clientAction = ClientAction(ActionType.CHALLENGE)
                    )
                    val authenticateReject = AuthenticateResponse.AuthenticateReject(
                        threeDSStatus = ThreeDSStatus.ThreeDS(
                            ThreeDSInformation(
                                exemptionStatus = ExemptionStatus.ExemptionNotAccepted,
                                version = ThreeDSVersion("2.1"),
                                eci = ECI(5)
                            )
                        ),
                        pspReference = PSPReference("pspReference"),
                        errorDescription = "errorDescription",
                        errorCode = "errorCode",
                        errorReason = ErrorReason.AUTHORIZATION_ERROR,
                        rejectionUseCase = RejectionUseCase.UNDEFINED
                    )
                    val authenticateSuccess = AuthenticateResponse.AuthenticateSuccess(
                        threeDSStatus = ThreeDSStatus.ThreeDS(
                            ThreeDSInformation(
                                exemptionStatus = ExemptionStatus.ExemptionNotAccepted,
                                version = ThreeDSVersion("2.1"),
                                eci = ECI(2)
                            )
                        ),
                        pspReference = PSPReference("pspReference")
                    )
                    val authorizeSuccess = AuthorizeResponse.AuthorizeSuccess(
                        pspReference = PSPReference("pspReference")
                    )

                    every { riskService.assessRisk(any()) }
                        .returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.AUTHENTICATION_MANDATORY) )

                    every { routingService.routeForPayment(any()) }
                        .returns( RoutingResult.Proceed(PaymentAccount(AccountId("id1"))) )
                        .andThen( RoutingResult.Proceed(PaymentAccount(AccountId("id2"))) )

                    // (authon) fingerprint -> (confirm) challenge -> (confirm) reject ->
                    // (authon) challenge -> (confirm) success --> (authze) -> success

                    every { authorizationGateway.authenticate(any()) }
                        .returns( authenticateClientActionFingerprint )
                        .andThen( authenticateClientActionChallenge )

                    every { authorizationGateway.confirmAuthenticate( any()) }
                        .returns( authenticateClientActionChallenge )
                        .andThen( authenticateReject )
                        .andThen ( authenticateSuccess )

                    every { authorizationGateway.authorize( any() ) }
                        .returns(authorizeSuccess)

                    underTest.authorize(paymentPayload)
                    underTest.confirm(paymentId, mapOf())
                    underTest.confirm(paymentId, mapOf("ECI" to "05"))
                    underTest.confirm(paymentId, mapOf("ECI" to "02"))

                    printPaymentInfo(paymentId)
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
