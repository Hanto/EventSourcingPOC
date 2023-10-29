package domain.repositories

import domain.payment.Payment
import domain.payment.PaymentId

interface PaymentRepository
{
    fun save(payment: Payment)
    fun load(paymentId: PaymentId): Payment?
}
