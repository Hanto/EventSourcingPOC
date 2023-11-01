package domain.services.routing

import domain.payment.data.paymentaccount.PaymentAccount

sealed class RoutingResult
{
    data class Proceed(val account: PaymentAccount) : RoutingResult()
    data object Reject : RoutingResult()

    sealed class RoutingError : RoutingResult()
    {
        data object BankAccountNotFound : RoutingError()
        data object InvalidCurrency: RoutingError()
    }
}
