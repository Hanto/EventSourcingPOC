package domain.repositories

import domain.authorize.status.PaymentStatus
import domain.payment.PaymentId

interface PaymentRepository
{
    fun save(payment: PaymentStatus)
    fun load(paymentId: PaymentId): PaymentStatus?
}
