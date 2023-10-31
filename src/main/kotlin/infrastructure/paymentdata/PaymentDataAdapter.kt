package infrastructure.paymentdata

import domain.payment.lifecycle.events.PaymentEvent

class PaymentDataAdapter
{
    fun toPayment(paymentData: PaymentData): List<PaymentEvent>
    {
        TODO()
    }
}
