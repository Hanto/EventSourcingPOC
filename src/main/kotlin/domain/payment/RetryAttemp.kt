package domain.payment

data class RetryAttemp
(
    private val value: Int
)
{
    companion object
    {
        private const val MAX_RETRIES = 1

        @JvmStatic
        fun firstNormalAttemp() =
            RetryAttemp(0)
    }

    fun next(): RetryAttemp =
        RetryAttemp(value + 1)

    fun canRetry(): Boolean =
        value < MAX_RETRIES
}
