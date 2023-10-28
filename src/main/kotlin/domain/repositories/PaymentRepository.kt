package domain.repositories

import domain.authorize.events.PaymentEvent
import domain.payment.Payment
import domain.payment.PaymentId

interface PaymentRepository
{
    fun save(payment: Payment)
    fun load(paymentId: PaymentId): List<PaymentEvent>?
}
