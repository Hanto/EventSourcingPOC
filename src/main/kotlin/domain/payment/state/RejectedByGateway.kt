package domain.payment.state

sealed interface RejectedByGateway: Payment
{
    fun prepareForRetry(): Payment
}
