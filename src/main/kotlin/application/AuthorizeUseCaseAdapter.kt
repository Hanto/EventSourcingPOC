package application

import domain.payment.data.paymentpayload.PaymentPayload
import domain.payment.state.*

class AuthorizeUseCaseAdapter
{
    fun toAuthorizeUseCase(payment: Payment): AuthorizeUseCaseResponse =

        when (payment)
        {
            is ReadyForPaymentRequest -> AuthorizeUseCaseResponse.Failed(reason = "Impossible state: ReadyForPaymentRequest", false, null)

            is Authorized -> AuthorizeUseCaseResponse.Authorized(payment.payload.id)
            is Captured -> AuthorizeUseCaseResponse.Authorized(payment.payload.id)

            is Failed -> AuthorizeUseCaseResponse.Failed(payment.reason, false, null)

            is ReadyForAuthenticationAndAuthorization -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyForAuthentication -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyForAuthenticationAndAuthorizeClientAction -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyForAuthenticationAndAuthorizeConfirm -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyForAuthenticationClientAction -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyForAuthenticationConfirm -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyForAuthorization -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyForRisk -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyForRoutingInitial -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyForRoutingRetry -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyToDecideAuthMethod -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyForECIVerfication -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)

            is RejectedByAuthentication -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is RejectedByAuthorization -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is RejectedByECIVerification -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)
            is ReadyForCaptureVerification -> AuthorizeUseCaseResponse.InvalidPaymentStatus(payment.payload.id)

            is RejectedByRisk -> AuthorizeUseCaseResponse.RejectedByFraudEvaluation(payment)
            is RejectedByRouting -> AuthorizeUseCaseResponse.RejectedByRouting(payment)
            is RejectedByRoutingSameAccount -> AuthorizeUseCaseResponse.RejectedByGatewayAndNoMoreRetries(payment)
            is RejectedByGatewayAndNotRetriable -> AuthorizeUseCaseResponse.RejectedByGatewayAndNoMoreRetries(payment)
        }

    fun toPayment(paymentPayload: PaymentPayload): Payment =

        ReadyForPaymentRequest().addPaymentPayload(paymentPayload)
}
