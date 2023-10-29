package infrastructure

import domain.authorize.events.PaymentEvent
import domain.authorize.status.PaymentStatus
import domain.authorize.status.ReadyForPaymentRequest
import domain.payment.PaymentId
import domain.repositories.PaymentRepository

class PaymentRepositoryMemory: PaymentRepository
{
    private val map: MutableMap<PaymentId, MutableList<PaymentEvent>> = mutableMapOf()

    override fun save(payment: PaymentStatus)
    {
        val savedVersion = map[payment.paymentPayload?.paymentId!!]?.last()?.version

        if (savedVersion?.let { it != payment.baseVersion } == true)
            throw RuntimeException("OptimisticLockException: base version: $savedVersion doesn't match stored version: ${payment.baseVersion}")

        val events = map.getOrPut(payment.paymentPayload?.paymentId!!) { mutableListOf() }
        events.addAll(payment.newEvents)
    }

    override fun load(paymentId: PaymentId): PaymentStatus?
    {
        val events = map[paymentId]
        val payment = ReadyForPaymentRequest()

        return payment.let { events?.fold(payment as PaymentStatus) { payment, event -> payment.applyRecordedEvent(event) } }
    }
}
