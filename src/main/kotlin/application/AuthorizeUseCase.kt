package application

import domain.authorize.status.*
import domain.authorize.steps.fraud.RiskAssessmentService
import domain.authorize.steps.gateway.AuthorizationGateway
import domain.authorize.steps.routing.RoutingService
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
    fun authorize(paymentPayload: PaymentPayload): PaymentStatus
    {
        val payment = ReadyForPaymentRequest().addPaymentPayload(paymentPayload)
            .letIf( ReadyForRisk::class.java ) { it.addFraudAnalysisResult(riskService.assessRisk(it)) }
            .letIf( ReadyForRouting::class.java ) { tryToAuthorize(it) }

        paymentRepository.save(payment)
        return payment
    }

    private fun tryToAuthorize(input: ReadyForAnyRouting): PaymentStatus
    {
        return input.addRoutingResult(routingService.routeForPayment(input))
            .letIf( ReadyForAuthorization::class.java ) { it.addAuthorizeResponse(authorizeService.authorize(it)) }
            .letIf( ReadyForRoutingRetry::class.java ) { tryToAuthorize(it) }
    }

    fun confirm(paymentId: PaymentId, confirmParams: Map<String, Any>): PaymentStatus
    {
        val payment = paymentRepository.load(paymentId)!!
            .letIf( ReadyForClientActionResponse::class.java ) { it.addConfirmParameters(confirmParams) }
            .letIf( ReadyForConfirm::class.java ) { it.addConfirmResponse(authorizeService.confirm(it)) }
            .letIf( ReadyForRoutingRetry::class.java ) { tryToAuthorize(it) }

        paymentRepository.save(payment)
        return payment
    }

    // HELPER:
    //------------------------------------------------------------------------------------------------------------------

    private inline fun <reified T>PaymentStatus.letIf(classz: Class<T>, function: (T) -> PaymentStatus): PaymentStatus =

        if (this is T)
            function.invoke(this as T)
        else this
}
