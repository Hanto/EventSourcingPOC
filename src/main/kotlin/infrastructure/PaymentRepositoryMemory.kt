package infrastructure

import domain.authorize.events.PaymentEvent
import domain.payment.Payment
import domain.payment.PaymentId
import domain.repositories.PaymentRepository

class PaymentRepositoryMemory: PaymentRepository
{
    private val map: MutableMap<PaymentId, MutableList<PaymentEvent>> = mutableMapOf()

    override fun save(payment: Payment)
    {
        val savedVersion = map[payment.getPaymentId()]?.last()?.version

        if (savedVersion?.let { it != payment.baseVersion } == true)
            throw RuntimeException("OptimisticLockException: base version doesn't match stored version")

        val events = map.getOrPut(payment.getPaymentPayload().paymentId) { mutableListOf() }
        events.addAll(payment.getNewEvents())
    }

    override fun load(paymentId: PaymentId): List<PaymentEvent>? =

        map[paymentId]
}
