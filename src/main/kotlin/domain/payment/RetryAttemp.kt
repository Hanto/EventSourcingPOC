package domain.payment

data class RetryAttemp
(
    private val value: Int
)
{
    companion object
    {
        @JvmStatic
        fun firstNormalAttemp() =
            RetryAttemp(0)
    }

    fun next(): RetryAttemp =
        RetryAttemp(value + 1)

    fun isLessThan(int: Int): Boolean =
        value < int
}
