package infrastructure.paymentdata

import domain.payment.lifecycle.status.Payment
import domain.repositories.PaymentRepository

class PaymentDataRepository
(
    private val paymentRepository: PaymentRepository,
    private val paymentAdapter: PaymentAdapter,
)
{
    fun save(input: Payment): PaymentData
    {
        val events = paymentRepository.loadEvents(input.payload().paymentId)

        return paymentAdapter.toPaymentData(events)
    }
}
