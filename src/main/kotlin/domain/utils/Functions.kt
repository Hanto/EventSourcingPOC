package domain.utils

fun <T>T.letIf(predicate: (T) -> Boolean, function: (T) -> T ): T =

    if (predicate.invoke(this))
        function.invoke(this)
    else this
