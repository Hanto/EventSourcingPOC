package domain.repositories

import domain.payment.data.paymentpayload.PaymentId
import domain.payment.paymentevents.PaymentEvent
import domain.payment.state.Payment

interface PaymentRepositoryNew
{
    fun save(payment: Payment): Payment
    fun load(paymentId: PaymentId): Payment?
    fun loadEvents(paymentId: PaymentId): List<PaymentEvent>
}
