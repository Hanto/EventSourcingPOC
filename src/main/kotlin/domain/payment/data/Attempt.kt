package domain.payment.data

import domain.payment.state.Payment

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
        value > 0

    fun generateAttemptReference(payment: Payment): AttemptReference
    {
        val suffix = if (this.didRetry()) "R" else ""
        return AttemptReference("${payment.payload().authorizationReference.value}$suffix")
    }
}
