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
    // NON FUNCTIONAL WAY:
    //------------------------------------------------------------------------------------------------------------------

    fun authorizeOld(paymentPayload: PaymentPayload): Payment
    {
        var payment = Payment().addPaymentPayload(paymentPayload)

        payment = payment.addFraudAnalysisResult(riskService.assessRisk(payment))

        if (payment.authorizationStatus is ReadyForRouting)
            payment = tryToAuthorizeOld(payment)

        paymentRepository.save(payment)
        return payment
    }

    private fun tryToAuthorizeOld(input: Payment): Payment
    {
        var payment = input.addRoutingResult(routingService.routeForPayment(input))

        if (payment.authorizationStatus is ReadyForAuthorization)
        {
            payment = payment.addAuthorizeResponse(authorizeService.authorize(payment))

            if (payment.authorizationStatus is ReadyForRoutingRetry)
                payment = tryToAuthorizeOld(payment)
        }
        return payment
    }

    fun confirmOld(paymentId: PaymentId, confirmParams: Map<String, Any>): Payment
    {
        var payment = paymentRepository.load(paymentId)!!

        payment = payment.addConfirmResponse(authorizeService.confirm(payment), confirmParams)

        if (payment.authorizationStatus is ReadyForRoutingRetry)
            payment = tryToAuthorizeOld(payment)

        paymentRepository.save(payment)
        return payment
    }

    // FUNCTIONAL WAY:
    //------------------------------------------------------------------------------------------------------------------

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
        val payment = paymentRepository.load(paymentId)!!

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
        var payment = Payment()
        payment = payment.addPaymentPayload(paymentPayload)

        return when (payment.authorizationStatus)
        {
            is ReadyForRisk -> payment.right()
            else -> payment.left()
        }
    }

    private fun riskEvaluationStep(input: Payment): Either<Payment, Payment>
    {
        val payment = input.addFraudAnalysisResult(riskService.assessRisk(input))

        return when (payment.authorizationStatus)
        {
            is ReadyForRouting, is ReadyForRoutingRetry -> payment.right()
            else -> payment.left()
        }
    }

    private fun routingStep(input: Payment): Either<Payment, Payment>
    {
        val payment = input.addRoutingResult(routingService.routeForPayment(input))

        return when (payment.authorizationStatus)
        {
            is ReadyForAuthorization -> payment.right()
            else -> payment.left()
        }
    }

    private fun authorizationStep(input: Payment): Either<Payment, Payment>
    {
        val payment = input.addAuthorizeResponse(authorizeService.authorize(input))

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

    private fun confirmStep(input: Payment, confirmParams: Map<String, Any>): Either<Payment, Payment>
    {
        val payment = input.addConfirmResponse(authorizeService.confirm(input), confirmParams)

        return when (payment.authorizationStatus)
        {
            is Authorized -> payment.right()
            else -> payment.left()
        }
    }
}
