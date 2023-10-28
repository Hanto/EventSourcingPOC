package domain.payment

data class PaymentPayload
(
    val paymentId: PaymentId,
    val customer: Customer
)
{
}
