package domain.payment.payload

import domain.payment.payload.paymentmethod.PaymentMethod


data class PaymentPayload
(
    val paymentId: PaymentId,
    val authorizationReference: AuthorizationReference,
    val customer: Customer,
    val paymentMethod: PaymentMethod,
    val authorizationType: AuthorizationType
)
