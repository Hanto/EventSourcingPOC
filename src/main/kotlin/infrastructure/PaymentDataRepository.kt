package infrastructure

import domain.payment.lifecycle.status.Payment
import domain.repositories.PaymentRepository

class PaymentDataRepository
(
    private val paymentRepository: PaymentRepository
)
{
    fun save(payment: Payment)
    {
        val events = paymentRepository.loadEvents(payment.payload?.paymentId!!)
    }
}
