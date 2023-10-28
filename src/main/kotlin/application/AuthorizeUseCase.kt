package application

import arrow.core.*
import domain.authorize.status.*
import domain.authorize.steps.fraud.RiskAssessmentService
import domain.authorize.steps.gateway.AuthorizationGateway
import domain.authorize.steps.routing.RoutingService
import domain.payment.Payment
import domain.payment.PaymentId
import domain.payment.PaymentPayload
import domain.repositories.PaymentRepository

class AuthorizeUseCase
(
    private val riskService: RiskAssessmentService,
    private val routingService: RoutingService,
    private val authorizeService: AuthorizationGateway,
    private val paymentRepository: PaymentRepository,
)
{
    fun authorize(paymentPayload: PaymentPayload): Payment
    {
        val payment = paymentRequestedStep(paymentPayload)
            .flatMap { readyForRisk -> riskEvaluationStep(readyForRisk) }
            .flatMap { readyForRouting -> tryToAuthorize(readyForRouting) }
            .fold(::identity, ::identity)

        paymentRepository.save(payment)
        return payment
    }

    private fun tryToAuthorize(payment: Payment): Either<Payment, Payment> =

        routingStep(payment)
            .flatMap { readyForAuthorization -> authorizationStep(readyForAuthorization) }
            .handleErrorWith { rejectedByGateway -> retryStep(rejectedByGateway)
                .flatMap { readyForRouting -> tryToAuthorize(readyForRouting) } }

    fun confirm(paymentId: PaymentId, confirmParams: Map<String, Any>): Payment
    {
        val payment = loadPaymentStep(paymentId)

        confirmStep(payment, confirmParams)
            .handleErrorWith { rejectedByGateway -> retryStep(rejectedByGateway)
                .flatMap { retriable -> tryToAuthorize(retriable) } }
            .fold(::identity, ::identity)

        paymentRepository.save(payment)
        return payment
    }

    // STEPS:
    //------------------------------------------------------------------------------------------------------------------

    private fun paymentRequestedStep(paymentPayload: PaymentPayload): Either<Payment, Payment>
    {
        val payment = Payment(paymentPayload)

        return when (payment.authorizationStatus)
        {
            is ReadyForRisk -> payment.right()
            else -> payment.left()
        }
    }

    private fun riskEvaluationStep(payment: Payment): Either<Payment, Payment>
    {
        payment.addFraudAnalysisResult(riskService.assessRisk(payment))

        return when (payment.authorizationStatus)
        {
            is ReadyForRouting, is ReadyForRoutingRetry -> payment.right()
            else -> payment.left()
        }
    }

    private fun routingStep(payment: Payment): Either<Payment, Payment>
    {
        payment.addRoutingResult(routingService.routeForPayment(payment))

        return when (payment.authorizationStatus)
        {
            is ReadyForAuthorization -> payment.right()
            else -> payment.left()
        }
    }

    private fun authorizationStep(payment: Payment): Either<Payment, Payment>
    {
        payment.addAuthorizeResponse(authorizeService.authorize(payment))

        return when (payment.authorizationStatus)
        {
            is Authorized, is ReadyForClientActionResponse -> payment.right()
            else -> payment.left()
        }
    }

    private fun retryStep(payment: Payment): Either<Payment, Payment>
    {
        return when (payment.authorizationStatus)
        {
            is ReadyForRoutingRetry -> payment.right()
            else -> payment.left()
        }
    }

    private fun loadPaymentStep(paymentId: PaymentId): Payment
    {
        val events = paymentRepository.load(paymentId)
        return Payment(events!!)
    }

    private fun confirmStep(payment: Payment, confirmParams: Map<String, Any>): Either<Payment, Payment>
    {
        payment.addConfirmResponse(authorizeService.confirm(payment), confirmParams)

        return when (payment.authorizationStatus)
        {
            is Authorized -> payment.right()
            else -> payment.left()
        }
    }

    // NON FUNCTIONAL WAY:
    //------------------------------------------------------------------------------------------------------------------

    fun authorizeOld(paymentPayload: PaymentPayload): Payment
    {
        val payment = Payment(paymentPayload)

        payment.addFraudAnalysisResult(riskService.assessRisk(payment))

        if (payment.authorizationStatus is ReadyForRouting)
            tryToAuthorizeOld(payment)

        paymentRepository.save(payment)
        return payment
    }

    private fun tryToAuthorizeOld(payment: Payment)
    {
        payment.addRoutingResult(routingService.routeForPayment(payment))

        if (payment.authorizationStatus is ReadyForAuthorization)
        {
            payment.addAuthorizeResponse(authorizeService.authorize(payment))

            if (payment.authorizationStatus is ReadyForRoutingRetry)
                tryToAuthorizeOld(payment)
        }
    }

    fun confirmOld(paymentId: PaymentId, confirmParams: Map<String, Any>): Payment
    {
        val payment = Payment(paymentRepository.load(paymentId)!!)

        payment.addConfirmResponse(authorizeService.confirm(payment), confirmParams)

        if (payment.authorizationStatus is ReadyForRoutingRetry)
            tryToAuthorizeOld(payment)

        paymentRepository.save(payment)
        return payment
    }
}
