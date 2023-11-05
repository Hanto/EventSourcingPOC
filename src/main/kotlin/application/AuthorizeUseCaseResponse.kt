package application

import domain.payment.data.paymentpayload.PaymentId
import domain.payment.state.Rejected
import domain.services.gateway.ClientAction

sealed class AuthorizeUseCaseResponse
{
    data class Authorized(val paymentId: PaymentId) : AuthorizeUseCaseResponse()
    data class RejectedByFraudEvaluation(val rejected: Rejected) : AuthorizeUseCaseResponse()
    data class RejectedByRouting(val rejected: Rejected) : AuthorizeUseCaseResponse()
    data class RejectedByGateway(val rejected: Rejected) : AuthorizeUseCaseResponse()
    data class RejectedByGatewayAndNoMoreRetries(val rejected: Rejected) : AuthorizeUseCaseResponse()
    data class ClientActionFromGateway(val clientAction: ClientAction) : AuthorizeUseCaseResponse()
    data class InvalidPaymentStatus(val paymentId: PaymentId) : AuthorizeUseCaseResponse()
    data class Failed(val reason: String, val timeout: Boolean = false, val exception: Throwable? = null) : AuthorizeUseCaseResponse()
}
