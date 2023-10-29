package application

import domain.authorize.status.ReadyForAuthorization
import domain.authorize.status.ReadyForRouting
import domain.authorize.status.ReadyForRoutingRetry
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
        val payment = Payment()
            .let { it.addPaymentPayload(paymentPayload) }
            .let { it.addFraudAnalysisResult(riskService.assessRisk(it)) }
            .letIf ({ it.paymentStatus is ReadyForRouting }, { tryToAuthorize(it) })

        paymentRepository.save(payment)
        return payment
    }

    private fun tryToAuthorize(input: Payment): Payment
    {
        return input.addRoutingResult(routingService.routeForPayment(input))
            .letIf ({ it.paymentStatus is ReadyForAuthorization }, { it.addAuthorizeResponse(authorizeService.authorize(it)) })
            .letIf ({ it.paymentStatus is ReadyForRoutingRetry }, { tryToAuthorize(it) })
    }

    fun confirm(paymentId: PaymentId, confirmParams: Map<String, Any>): Payment
    {
        val payment = paymentRepository.load(paymentId)!!
            .let { it.addConfirmResponse(authorizeService.confirm(it), confirmParams) }
            .letIf ({ it.paymentStatus is ReadyForRoutingRetry }, { tryToAuthorize(it) })

        paymentRepository.save(payment)
        return payment
    }

    // HELPER:
    //------------------------------------------------------------------------------------------------------------------

    private fun <T>T.letIf(predicate: (T) -> Boolean, function: (T) -> T ): T =

        if (predicate.invoke(this))
            function.invoke(this)
        else this
}
