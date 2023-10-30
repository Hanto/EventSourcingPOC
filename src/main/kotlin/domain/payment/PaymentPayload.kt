package domain.payment

import domain.payment.payload.AuthorizationReference
import domain.payment.payload.Customer
import domain.payment.payload.PaymentId
import domain.payment.payload.paymentmethod.PaymentMethod


data class PaymentPayload
(
    val paymentId: PaymentId,
    val authorizationReference: AuthorizationReference,
    val customer: Customer,
    val paymentMethod: PaymentMethod
)
