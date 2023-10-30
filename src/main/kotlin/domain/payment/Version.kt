package domain.payment

import domain.authorize.events.PaymentEvent

data class Version
(
    val value: Int
)
{
    companion object
    {
        @JvmStatic
        fun firstVersion() =
            Version(0)
    }

    fun updateToEventVersionIfReplay(event: PaymentEvent, isNew: Boolean): Version =
        if (isNew) this else Version(event.version.value)

    fun updateToLatestEventVersion(events: List<PaymentEvent>): Version =
        Version(events.maxByOrNull { it.version.value }?.version?.value ?: value)

    fun isSameVersion(version: Version?): Boolean =
        version?.let { version.value == value } ?: true

    fun nextEventVersion(events: List<PaymentEvent>): Version =
        Version(value + events.size + 1)
}
