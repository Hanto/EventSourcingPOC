package domain.payment.lifecycle.events

import java.util.*

data class PaymentEventId
(
    val id: UUID = UUID.randomUUID()
)
