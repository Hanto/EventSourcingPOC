package domain.payment

data class RetryAttemp
(
    val value: Int
)
{
    companion object
    {
        @JvmStatic
        fun firstNormalAttemp() =
            RetryAttemp(0)
    }

    fun next(): RetryAttemp =
        RetryAttemp(value +1)
}
