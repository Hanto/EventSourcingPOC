package domain.payment.state

sealed interface RejectedByGateway
{
    fun prepareForRetry(): Payment
}
