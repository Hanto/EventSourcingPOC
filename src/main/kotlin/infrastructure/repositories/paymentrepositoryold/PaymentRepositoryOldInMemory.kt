package infrastructure.repositories.paymentrepositoryold

import domain.payment.data.paymentpayload.PaymentId
import domain.payment.state.Payment
import domain.repositories.PaymentRepositoryNew
import domain.repositories.PaymentRepositoryOld

class PaymentRepositoryOldInMemory
(
    private val paymentRepositoryNew: PaymentRepositoryNew,
    private val paymentAdapter: PaymentAdapter,

): PaymentRepositoryOld
{
    private val map: MutableMap<PaymentId, PaymentData> = mutableMapOf()

    override fun save(payment: Payment)
    {
        println("saveOld: ${payment::class.java.simpleName}")

        val events = paymentRepositoryNew.loadEvents(payment.payload().paymentId)

        val paymentData = paymentAdapter.toPaymentData(payment, events)

        map[payment.payload().paymentId] = paymentData
    }

    override fun loadPaymentData(paymentId: PaymentId): PaymentData?
    {
        return map[paymentId]
    }
}
