package domain.utils

fun <T>T.letIf(predicate: (T) -> Boolean, function: (T) -> T ): T =

    if (predicate.invoke(this))
        function.invoke(this)
    else this

inline fun <reified T, R>R.letIf(function: (T) -> R): R =

    if (this is T)
        function.invoke(this as T)
    else this
