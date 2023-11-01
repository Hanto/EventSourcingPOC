package domain.payment.data.threedsinformation


data class ECI
(
    val value: Int
)
{
    // ALTERNATIVE CONSTRUCTOR:
    //--------------------------------------------------------------------------------------------------------

    companion object
    {
        operator fun invoke(value: String): ECI =

            ECI(value.toInt())
    }

    // VALIDATOR:
    //--------------------------------------------------------------------------------------------------------

    init
    {
        require(value in listOf(0,1,2,5,6,7)) { "ECI value not allowed: $value" }
    }

    // MAIN:
    //--------------------------------------------------------------------------------------------------------

    fun result(): EciResult =

        when (value)
        {
            2,5 -> EciResult.SUCCESSFUL
            1,6 -> EciResult.ATTEMPTED
            0,7 -> EciResult.REJECTED
            else -> throw RuntimeException("ECI value not allowed: $value")
        }

    // HELPER:
    //--------------------------------------------------------------------------------------------------------

    enum class EciResult
    {
        SUCCESSFUL,
        ATTEMPTED,
        REJECTED
    }
}
