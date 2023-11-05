package application

import domain.payment.data.paymentpayload.PaymentId
import domain.payment.data.paymentpayload.PaymentPayload
import domain.repositories.PaymentRepository
import domain.services.authorize.AuthorizeService

class AuthorizeUseCase
(
    private val authorizeService: AuthorizeService,
    private val paymentRepository: PaymentRepository,
    private val authorizeUseCaseAdapter: AuthorizeUseCaseAdapter,
)
{
    fun authorize(paymentPayload: PaymentPayload): AuthorizeUseCaseResponse =

        runCatching {

            val input = authorizeUseCaseAdapter.toPayment(paymentPayload)

            val result = authorizeService.authorize(input)

            return authorizeUseCaseAdapter.toAuthorizeUseCase(result)

        }.getOrElse {

            return AuthorizeUseCaseResponse.Failed(
                reason = it.message ?: it.javaClass.simpleName,
                timeout = false,
                exception = it
            )
        }

    fun confirm(paymentId: PaymentId, confirmParams: Map<String, Any>): AuthorizeUseCaseResponse =

        runCatching {

            val input = paymentRepository.load(paymentId)!!

            val result = authorizeService.confirm(input, confirmParams)

            return authorizeUseCaseAdapter.toAuthorizeUseCase(result)

        }.getOrElse {

            return AuthorizeUseCaseResponse.Failed(
                reason = it.message ?: it.javaClass.simpleName,
                timeout = false,
                exception = it
            )
        }
}
