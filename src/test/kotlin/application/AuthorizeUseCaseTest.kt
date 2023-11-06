package application

import domain.payment.data.PSPReference
import domain.payment.data.RiskAssessmentOutcome
import domain.payment.data.paymentaccount.AccountId
import domain.payment.data.paymentaccount.AuthorisationAction
import domain.payment.data.paymentaccount.AuthorisationAction.AuthorizationPreference.ECI_CHECK
import domain.payment.data.paymentaccount.AuthorisationAction.ExemptionPreference.DONT_TRY_EXEMPTION
import domain.payment.data.paymentaccount.PaymentAccount
import domain.payment.data.paymentpayload.*
import domain.payment.data.paymentpayload.paymentmethod.CreditCardPayment
import domain.payment.data.threedstatus.*
import domain.payment.state.Authorized
import domain.payment.state.ReadyForAuthenticationAndAuthorizeClientAction
import domain.payment.state.RejectedByGatewayAndNotRetriable
import domain.services.authorize.AuthorizeService
import domain.services.featureflag.FeatureFlag
import domain.services.featureflag.FeatureFlag.Feature.DECOUPLED_AUTH
import domain.services.fraud.FraudAnalysisResult
import domain.services.fraud.RiskAssessmentService
import domain.services.gateway.*
import domain.services.gateway.AuthenticateResponse.*
import domain.services.routing.RoutingResult
import domain.services.routing.RoutingService
import infrastructure.EventPublisherMemory
import infrastructure.repositories.paymentrepositorynew.PaymentRepositoryInMemory
import infrastructure.repositories.paymentrepositoryold.PaymentAdapter
import infrastructure.repositories.paymentrepositoryold.PaymentRepositoryLegacyInMemory
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class AuthorizeUseCaseTest
{
    private val riskService = mockk<RiskAssessmentService>()
    private val routingService = mockk<RoutingService>()
    private val authorizationGateway = mockk<AuthorizationGateway>()
    private val featureFlag = mockk<FeatureFlag>()
    private val eventPublisher = EventPublisherMemory()
    private val paymentRepositoryNew = PaymentRepositoryInMemory()
    private val paymentRepositoryOld = PaymentRepositoryLegacyInMemory(paymentRepositoryNew, PaymentAdapter())

    private val authorizeService = AuthorizeService(
        riskService = riskService,
        routingService = routingService,
        authorizeService = authorizationGateway,
        eventPublisher = eventPublisher,
        paymentRepository = paymentRepositoryNew,
        paymentRepositoryLegacy = paymentRepositoryOld,
        featureFlag = featureFlag
    )
    private val underTest = AuthorizeUseCase(
        authorizeService = authorizeService,
        paymentRepository = paymentRepositoryNew,
        authorizeUseCaseAdapter = AuthorizeUseCaseAdapter()
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
        every { featureFlag.isFeatureEnabledFor(DECOUPLED_AUTH) }.returns(false)
    }

    @Nested
    inner class No3DS
    {
        @Test
        fun whenNoRetry()
        {
            val routingResult1 = RoutingResult.Proceed(
                PaymentAccount(
                    accountId = AccountId("id1"),
                    authorisationAction = AuthorisationAction.Moto)
            )
            val threeDSStatus = ThreeDSStatus.NoThreeDS

            val authSuccess = AuthenticateAndAuthorizeSuccess(
                threeDSStatus = threeDSStatus,
                exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                pspReference = PSPReference("pspReference"))

            every { riskService.assessRisk(any()) }
                .returns( FraudAnalysisResult.Approved(RiskAssessmentOutcome.FRICTIONLESS) )

            every { routingService.routeForPayment(any()) }
                .returns( routingResult1 )

            every { authorizationGateway.authenticateAndAuthorize(any()) }
                .returns( authSuccess)

            underTest.authorize(paymentPayload)

            printPaymentInfo(paymentId)

            val result = paymentRepositoryNew.load(paymentId)

            assertThat(result).isInstanceOf(Authorized::class.java)
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
                    val routingResult1 = RoutingResult.Proceed(
                        PaymentAccount(
                            accountId = AccountId("id1"),
                            authorisationAction = AuthorisationAction.Moto)
                    )
                    val routingResult2 = RoutingResult.Proceed(
                        PaymentAccount(
                            accountId = AccountId("id2"),
                            authorisationAction = AuthorisationAction.Moto)
                    )
                    val threeDSStatus = ThreeDSStatus.NoThreeDS

                    val authReject = AuthenticateReject(
                        threeDSStatus = threeDSStatus,
                        exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                        pspReference = PSPReference("pspReference"),
                        errorDescription = "errorDescription",
                        errorCode = "errorCode",
                        errorReason = ErrorReason.AUTHORIZATION_ERROR,
                        rejectionUseCase =  RejectionUseCase.UNDEFINED
                    )
                    val authSuccess = AuthenticateAndAuthorizeSuccess(
                        threeDSStatus = threeDSStatus,
                        exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                        pspReference = PSPReference("pspReference")
                    )

                    every { riskService.assessRisk(any()) }
                        .returns( FraudAnalysisResult.Approved(RiskAssessmentOutcome.FRICTIONLESS) )

                    every { routingService.routeForPayment(any()) }
                        .returns( routingResult1 )
                        .andThen( routingResult2 )

                    every { authorizationGateway.authenticateAndAuthorize(any()) }
                        .returns( authReject)
                        .andThen( authSuccess )

                    underTest.authorize(paymentPayload)

                    printPaymentInfo(paymentId)

                    val result = paymentRepositoryNew.load(paymentId)

                    assertThat(result).isInstanceOf(Authorized::class.java)
                }

                @Test
                fun whenRetryAndDifferentAccount()
                {
                    val routingResult1 = RoutingResult.Proceed(
                        PaymentAccount(
                            accountId = AccountId("id1"),
                            authorisationAction = AuthorisationAction.Moto)
                    )
                    val routingResult2 = RoutingResult.Proceed(
                        PaymentAccount(
                            accountId = AccountId("id2"),
                            authorisationAction = AuthorisationAction.Moto)
                    )
                    val threeDSStatus = ThreeDSStatus.NoThreeDS

                    val authReject = AuthenticateReject(
                        threeDSStatus = threeDSStatus,
                        exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                        pspReference = PSPReference("pspReference"),
                        errorDescription = "errorDescription",
                        errorCode = "errorCode",
                        errorReason = ErrorReason.AUTHORIZATION_ERROR,
                        rejectionUseCase =  RejectionUseCase.UNDEFINED
                    )
                    val authSuccess = AuthenticateAndAuthorizeSuccess(
                        threeDSStatus = threeDSStatus,
                        exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                        pspReference = PSPReference("pspReference")
                    )

                    every { riskService.assessRisk(any()) }
                        .returns( FraudAnalysisResult.Approved(RiskAssessmentOutcome.FRICTIONLESS) )

                    every { routingService.routeForPayment(any()) }
                        .returns( routingResult1 )
                        .andThen( routingResult2 )

                    every { authorizationGateway.authenticateAndAuthorize(any()) }
                        .returns( authReject)
                        .andThen( authSuccess )

                    underTest.authorize(paymentPayload)

                    printPaymentInfo(paymentId)

                    val result = paymentRepositoryNew.load(paymentId)

                    assertThat(result).isInstanceOf(Authorized::class.java)
                }
            }

            @Test
            fun whenSecondRetry()
            {
                val routingResult1 = RoutingResult.Proceed(
                    PaymentAccount(
                        accountId = AccountId("id1"),
                        authorisationAction = AuthorisationAction.Moto)
                )
                val routingResult2 = RoutingResult.Proceed(
                    PaymentAccount(
                        accountId = AccountId("id2"),
                        authorisationAction = AuthorisationAction.Moto)
                )
                val threeDSStatus = ThreeDSStatus.NoThreeDS

                val authReject = AuthenticateReject(
                    threeDSStatus = threeDSStatus,
                    exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                    pspReference = PSPReference("pspReference"),
                    errorDescription = "errorDescription",
                    errorCode = "errorCode",
                    errorReason = ErrorReason.AUTHORIZATION_ERROR,
                    rejectionUseCase =  RejectionUseCase.UNDEFINED
                )
                val authSuccess = AuthenticateAndAuthorizeSuccess(
                    threeDSStatus = threeDSStatus,
                    exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                    pspReference = PSPReference("pspReference")
                )

                every { riskService.assessRisk(any()) }
                    .returns( FraudAnalysisResult.Approved(RiskAssessmentOutcome.FRICTIONLESS) )

                every { routingService.routeForPayment(any()) }
                    .returns( routingResult1 )
                    .andThen( routingResult2 )

                every { authorizationGateway.authenticateAndAuthorize(any()) }
                    .returns( authReject)
                    .andThen( authReject )
                    .andThen( authSuccess )

                underTest.authorize(paymentPayload)

                printPaymentInfo(paymentId)

                val result = paymentRepositoryNew.load(paymentId)

                assertThat(result).isInstanceOf(RejectedByGatewayAndNotRetriable::class.java)
            }
        }
    }

    @Nested
    inner class When3DS
    {
        @Test
        fun when3DSPending()
        {
            val routingResult1 = RoutingResult.Proceed(
                PaymentAccount(
                    accountId = AccountId("id1"),
                    authorisationAction = AuthorisationAction.ThreeDS(
                        exemptionPreference = DONT_TRY_EXEMPTION,
                        authorizationPreference = ECI_CHECK))
            )
            val threeDSStatus =  ThreeDSStatus.PendingThreeDS(
                version = ThreeDSVersion("2.1")
            )
            val authClientAction = AuthenticateClientAction(
                threeDSStatus = threeDSStatus,
                pspReference = PSPReference("pspReference"),
                exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                clientAction = ClientAction(ActionType.CHALLENGE))

            every { riskService.assessRisk(any()) }
                .returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.AUTHENTICATION_MANDATORY) )

            every { routingService.routeForPayment(any()) }
                .returns( routingResult1 )

            every { authorizationGateway.authenticateAndAuthorize(any()) }
                .returns( authClientAction )

            underTest.authorize(paymentPayload)

            printPaymentInfo(paymentId)

            val result = paymentRepositoryNew.load(paymentId)

            assertThat(result).isInstanceOf(ReadyForAuthenticationAndAuthorizeClientAction::class.java)
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
                        val routingResult1 = RoutingResult.Proceed(
                            PaymentAccount(
                                accountId = AccountId("id1"),
                                authorisationAction = AuthorisationAction.ThreeDS(
                                    exemptionPreference = DONT_TRY_EXEMPTION,
                                    authorizationPreference = ECI_CHECK))
                        )
                        val routingResult2 = RoutingResult.Proceed(
                            PaymentAccount(
                                accountId = AccountId("id2"),
                                authorisationAction =AuthorisationAction.ThreeDS(
                                    exemptionPreference = DONT_TRY_EXEMPTION,
                                    authorizationPreference = ECI_CHECK))
                        )
                        val authClientAction = AuthenticateClientAction(
                            threeDSStatus = ThreeDSStatus.PendingThreeDS(
                                version = ThreeDSVersion("2.1")
                            ),
                            pspReference = PSPReference("pspReference"),
                            exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                            clientAction = ClientAction(ActionType.CHALLENGE)
                        )
                        val authReject = AuthenticateReject(
                            threeDSStatus = ThreeDSStatus.ThreeDS(
                                version = ThreeDSVersion("2.1"),
                                eci = ECI(5),
                                transactionId = ThreeDSTransactionId("transactionId"),
                                cavv = CAVV("cavv"),
                                xid = XID("xid")
                            ),
                            exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                            pspReference = PSPReference("pspReference"),
                            errorDescription = "errorDescription",
                            errorCode = "errorCode",
                            errorReason = ErrorReason.AUTHORIZATION_ERROR,
                            rejectionUseCase =  RejectionUseCase.UNDEFINED
                        )
                        val authSuccess = AuthenticateAndAuthorizeSuccess(
                            threeDSStatus = ThreeDSStatus.ThreeDS(
                                version = ThreeDSVersion("2.1"),
                                eci = ECI(2),
                                transactionId = ThreeDSTransactionId("transactionId"),
                                cavv = CAVV("cavv"),
                                xid = XID("xid")
                            ),
                            exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                            pspReference = PSPReference("pspReference")
                        )

                        every { riskService.assessRisk(any()) }
                            .returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.AUTHENTICATION_MANDATORY) )

                        every { routingService.routeForPayment(any()) }
                            .returns( routingResult1 )
                            .andThen( routingResult2 )

                        every { authorizationGateway.authenticateAndAuthorize(any()) }
                            .returns( authClientAction )
                            .andThen( authSuccess )

                        every { authorizationGateway.confirmAuthenticateAndAuthorize( any()) }
                            .returns( authReject )

                        underTest.authorize(paymentPayload)
                        underTest.confirm(paymentId, mapOf("ECI" to "05"))

                        printPaymentInfo(paymentId)

                        val result = paymentRepositoryNew.load(paymentId)

                        assertThat(result).isInstanceOf(Authorized::class.java)
                    }

                    @Test
                    fun whenRetryAndDifferentAccount()
                    {
                        val routingResult1 = RoutingResult.Proceed(
                            PaymentAccount(
                                accountId = AccountId("id1"),
                                authorisationAction = AuthorisationAction.ThreeDS(
                                    exemptionPreference = DONT_TRY_EXEMPTION,
                                    authorizationPreference = ECI_CHECK))
                        )
                        val routingResult2 = RoutingResult.Proceed(
                            PaymentAccount(
                                accountId = AccountId("id2"),
                                authorisationAction = AuthorisationAction.ThreeDS(
                                    exemptionPreference = DONT_TRY_EXEMPTION,
                                    authorizationPreference = ECI_CHECK))
                        )
                        val authClientAction = AuthenticateClientAction(
                            threeDSStatus = ThreeDSStatus.PendingThreeDS(
                                version = ThreeDSVersion("2.1")
                            ),
                            exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                            pspReference = PSPReference("pspReference"),
                            clientAction = ClientAction(ActionType.CHALLENGE)
                        )
                        val authReject = AuthenticateReject(
                            threeDSStatus = ThreeDSStatus.ThreeDS(
                                version = ThreeDSVersion("2.1"),
                                eci = ECI(5),
                                transactionId = ThreeDSTransactionId("transactionId"),
                                cavv = CAVV("cavv"),
                                xid = XID("xid")
                            ),
                            exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                            pspReference = PSPReference("pspReference"),
                            errorDescription = "errorDescription",
                            errorCode = "errorCode",
                            errorReason = ErrorReason.AUTHORIZATION_ERROR,
                            rejectionUseCase =  RejectionUseCase.UNDEFINED
                        )
                        val authSuccess = AuthenticateAndAuthorizeSuccess(
                            threeDSStatus = ThreeDSStatus.ThreeDS(
                                version = ThreeDSVersion("2.1"),
                                eci = ECI(2),
                                transactionId = ThreeDSTransactionId("transactionId"),
                                cavv = CAVV("cavv"),
                                xid = XID("xid")
                            ),
                            exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                            pspReference = PSPReference("pspReference")
                        )

                        every { riskService.assessRisk(any()) }
                            .returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.AUTHENTICATION_MANDATORY) )

                        every { routingService.routeForPayment(any()) }
                            .returns( routingResult1 )
                            .andThen( routingResult2 )

                        every { authorizationGateway.authenticateAndAuthorize(any()) }
                            .returns( authClientAction )
                            .andThen( authSuccess )

                        every { authorizationGateway.confirmAuthenticateAndAuthorize( any()) }
                            .returns( authReject )

                        underTest.authorize(paymentPayload)
                        underTest.confirm(paymentId, mapOf("ECI" to "05"))

                        printPaymentInfo(paymentId)

                        val result = paymentRepositoryNew.load(paymentId)

                        assertThat(result).isInstanceOf(Authorized::class.java)
                    }
                }

                @Test
                fun whenSecondRetry()
                {
                    val routingResult1 = RoutingResult.Proceed(
                        PaymentAccount(
                            accountId = AccountId("id1"),
                            authorisationAction = AuthorisationAction.ThreeDS(
                                exemptionPreference = DONT_TRY_EXEMPTION,
                                authorizationPreference = ECI_CHECK))
                    )
                    val routingResult2 = RoutingResult.Proceed(
                        PaymentAccount(
                            accountId = AccountId("id2"),
                            authorisationAction = AuthorisationAction.ThreeDS(
                                exemptionPreference = DONT_TRY_EXEMPTION,
                                authorizationPreference = ECI_CHECK))
                    )
                    val authClientAction = AuthenticateClientAction(
                        threeDSStatus = ThreeDSStatus.PendingThreeDS(
                            version = ThreeDSVersion("2.1")
                        ),
                        exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                        pspReference = PSPReference("pspReference"),
                        clientAction = ClientAction(ActionType.CHALLENGE)
                    )
                    val authReject = AuthenticateReject(
                        threeDSStatus = ThreeDSStatus.ThreeDS(
                            version = ThreeDSVersion("2.1"),
                            eci = ECI(5),
                            transactionId = ThreeDSTransactionId("transactionId"),
                            cavv = CAVV("cavv"),
                            xid = XID("xid")
                        ),
                        exemptionStatus = ExemptionStatus.ExemptionNotRequested,
                        pspReference = PSPReference("pspReference"),
                        errorDescription = "errorDescription",
                        errorCode = "errorCode",
                        errorReason = ErrorReason.AUTHORIZATION_ERROR,
                        rejectionUseCase =  RejectionUseCase.UNDEFINED
                    )

                    every { riskService.assessRisk(any()) }
                        .returns( FraudAnalysisResult.Approved(riskAssessmentOutcome = RiskAssessmentOutcome.AUTHENTICATION_MANDATORY) )

                    every { routingService.routeForPayment(any()) }
                        .returns( routingResult1 )
                        .andThen( routingResult2 )

                    every { authorizationGateway.authenticateAndAuthorize(any()) }
                        .returns( authClientAction )
                        .andThen( authReject )

                    every { authorizationGateway.confirmAuthenticateAndAuthorize( any()) }
                        .returns( authReject )

                    underTest.authorize(paymentPayload)
                    underTest.confirm(paymentId, mapOf("ECI" to "05"))

                    printPaymentInfo(paymentId)

                    val result = paymentRepositoryNew.load(paymentId)

                    assertThat(result).isInstanceOf(RejectedByGatewayAndNotRetriable::class.java)

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
