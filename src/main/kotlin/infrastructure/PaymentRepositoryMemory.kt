package infrastructure

import domain.authorize.events.PaymentEvent
import domain.authorize.status.Payment
import domain.authorize.status.ReadyForPaymentRequest
import domain.payment.PaymentId
import domain.payment.Version
import domain.repositories.PaymentRepository

class PaymentRepositoryMemory: PaymentRepository
{
    private val map: MutableMap<PaymentId, MutableList<PaymentEvent>> = mutableMapOf()

    override fun save(payment: Payment): Payment
    {
        val savedVersion = map[payment.paymentPayload?.paymentId!!]?.last()?.version

        verifyDataConsistency(payment, savedVersion)

        val events = map.getOrPut(payment.paymentPayload?.paymentId!!) { mutableListOf() }
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
        if  (!payment.baseVersion.isSameVersion(savedVersion))
            throw RuntimeException("OptimisticLockException: base version: $savedVersion doesn't match stored version: ${payment.baseVersion}")
    }
}
