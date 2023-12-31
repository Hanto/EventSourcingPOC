package infrastructure.repositories.paymentrepositoryold

import domain.payment.data.paymentpayload.PaymentId
import domain.payment.state.Payment
import domain.repositories.PaymentRepository
import domain.repositories.PaymentRepositoryLegacy
import infrastructure.repositories.paymentrepositoryold.paymentdata.PaymentData

class PaymentRepositoryLegacyInMemory
(
    private val paymentRepository: PaymentRepository,
    private val paymentAdapter: PaymentAdapter,

    ): PaymentRepositoryLegacy
{
    private val map: MutableMap<PaymentId, PaymentData> = mutableMapOf()

    override fun save(payment: Payment)
    {
        println("saveOld: ${payment::class.java.simpleName}")

        val events = paymentRepository.loadEvents(payment.payload().id)

        val paymentData = paymentAdapter.toPaymentData(payment, events)

        map[payment.payload().id] = paymentData
    }

    override fun loadPaymentData(paymentId: PaymentId): PaymentData?
    {
        return map[paymentId]
    }
}
