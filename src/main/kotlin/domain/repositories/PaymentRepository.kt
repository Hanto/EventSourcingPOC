package domain.repositories

import domain.lifecycle.status.Payment
import domain.payment.payload.PaymentId

interface PaymentRepository
{
    fun save(payment: Payment): Payment
    fun load(paymentId: PaymentId): Payment?
}
