package domain.payment


data class PaymentPayload
(
    val paymentId: PaymentId,
    val authorizationReference: AuthorizationReference,
    val customer: Customer
)
