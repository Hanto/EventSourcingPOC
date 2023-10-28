package domain.authorize.events

sealed interface PaymentEvent
{
    val version: Int
}
