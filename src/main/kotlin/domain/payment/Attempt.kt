package domain.payment

data class Attempt
(
    private val value: Int
)
{
    companion object
    {
        private const val MAX_RETRIES = 1

        @JvmStatic
        fun firstNormalAttemp() =
            Attempt(0)
    }

    fun next(): Attempt =
        Attempt(value + 1)

    fun canRetry(): Boolean =
        value < MAX_RETRIES

    fun didRetry(): Boolean =
        value > 0;
}
