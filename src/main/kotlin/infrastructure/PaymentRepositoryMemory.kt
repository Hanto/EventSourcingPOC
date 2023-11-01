package infrastructure

import domain.payment.Version
import domain.payment.lifecycle.events.PaymentEvent
import domain.payment.lifecycle.status.Payment
import domain.payment.lifecycle.status.ReadyForPaymentRequest
import domain.payment.payload.PaymentId
import domain.repositories.PaymentRepository

class PaymentRepositoryMemory: PaymentRepository
{
    private val map: MutableMap<PaymentId, MutableList<PaymentEvent>> = mutableMapOf()

    override fun save(payment: Payment): Payment
    {
        println("save: ${payment::class.java.simpleName}")

        val events = map.getOrPut(payment.payload().paymentId) { mutableListOf() }
        val savedVersion = events.map { it.version }.maxByOrNull { it.value }

        verifyDataConsistency(payment, savedVersion)

        events.addAll(payment.paymentEvents)

        return payment.flushPaymentEvents()
    }

    override fun load(paymentId: PaymentId): Payment?
    {
        val events = loadEvents(paymentId).sortedBy { it.version.value }
        val payment: Payment = ReadyForPaymentRequest()

        return events.fold (payment) { it, event -> it.applyRecordedEvent(event) }
    }

    override fun loadEvents(paymentId: PaymentId): List<PaymentEvent> =

        map.getOrDefault(paymentId, emptyList())

    // HELPER:
    //------------------------------------------------------------------------------------------------------------------

    private fun verifyDataConsistency(payment: Payment, savedVersion: Version?)
    {
        if  (!payment.version.isSameVersion(savedVersion))
            throw RuntimeException("OptimisticLockException: base version: $savedVersion doesn't match stored version: ${payment.version}")
    }
}
