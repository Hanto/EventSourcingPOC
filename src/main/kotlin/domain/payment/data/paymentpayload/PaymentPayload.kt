package domain.payment.data.paymentpayload

import domain.payment.data.paymentpayload.paymentmethod.PaymentMethod


data class PaymentPayload
(
    val paymentId: PaymentId,
    val authorizationReference: AuthorizationReference,
    val customer: Customer,
    val paymentMethod: PaymentMethod,
    val authorizationType: AuthorizationType
)
