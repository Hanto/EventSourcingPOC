package infrastructure

import domain.lifecycle.events.PaymentEvent
import domain.lifecycle.status.Payment
import domain.lifecycle.status.ReadyForPaymentRequest
import domain.payment.Version
import domain.payment.payload.PaymentId
import domain.repositories.PaymentRepository

class PaymentRepositoryMemory: PaymentRepository
{
    private val map: MutableMap<PaymentId, MutableList<PaymentEvent>> = mutableMapOf()

    override fun save(payment: Payment): Payment
    {
        val savedVersion = map[payment.payload?.paymentId!!]?.last()?.version

        verifyDataConsistency(payment, savedVersion)

        val events = map.getOrPut(payment.payload?.paymentId!!) { mutableListOf() }
        events.addAll(payment.paymentEvents)

        return payment.flushPaymentEvents()
    }

    override fun load(paymentId: PaymentId): Payment?
    {
        val events = map[paymentId]
        val payment = ReadyForPaymentRequest()

        return payment.let { events?.fold(payment as Payment) { payment, event -> payment.applyRecordedEvent(event) } }
    }

    fun loadEvents(paymentId: PaymentId): List<PaymentEvent> =

        map.getOrDefault(paymentId, emptyList())

    private fun verifyDataConsistency(payment: Payment, savedVersion: Version?)
    {
        if  (!payment.version.isSameVersion(savedVersion))
            throw RuntimeException("OptimisticLockException: base version: $savedVersion doesn't match stored version: ${payment.version}")
    }
}
