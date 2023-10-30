package domain.payment

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

    fun updateToEventVersionIfReplay(event: Versionable, isNew: Boolean): Version =
        if (isNew) this else Version(event.version.value)

    fun updateToLatestEventVersion(events: List<Versionable>): Version =
        Version(events.maxByOrNull { it.version.value }?.version?.value ?: value)

    fun isSameVersion(version: Version?): Boolean =
        version?.let { version.value == value } ?: true

    fun nextEventVersion(events: List<Versionable>): Version =
        Version(value + events.size + 1)
}
