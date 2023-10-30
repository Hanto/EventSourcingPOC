package domain.events

class SideEffectEventList
(
    list: List<SideEffectEvent>
)
{
    val list = list.toMutableList()

    fun addIfNew(event: SideEffectEvent, isNew: Boolean)
    {
        if (isNew)
            list.add(event)
    }
}
