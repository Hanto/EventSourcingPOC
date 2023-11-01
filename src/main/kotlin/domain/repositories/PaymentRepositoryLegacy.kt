package domain.repositories

import domain.payment.data.paymentpayload.PaymentId
import domain.payment.state.Payment
import infrastructure.repositories.paymentrepositoryold.paymentdata.PaymentData

interface PaymentRepositoryLegacy
{
    fun save(payment: Payment)
    fun loadPaymentData(paymentId: PaymentId): PaymentData?
}
