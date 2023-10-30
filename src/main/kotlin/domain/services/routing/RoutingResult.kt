package domain.services.routing

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
