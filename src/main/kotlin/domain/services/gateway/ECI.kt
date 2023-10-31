package domain.services.gateway


data class ECI
(
    val value: Int?
)
{
    // ALTERNATIVE CONSTRUCTOR:
    //--------------------------------------------------------------------------------------------------------

    companion object
    {
        operator fun invoke(value: String?): ECI =

            ECI(value?.ifEmpty { null }?.toInt())
    }

    // VALIDATOR:
    //--------------------------------------------------------------------------------------------------------

    init
    {
        require(value in listOf(null,0,1,2,5,6,7)) { "ECI value not allowed: $value" }
    }

    // MAIN:
    //--------------------------------------------------------------------------------------------------------

    fun outcome(): EciOutcome =

        when (value)
        {
            null -> NoThreeDS
            2,5 -> ThreeDS(EciResult.SUCCESSFUL)
            1,6 -> ThreeDS(EciResult.ATTEMPTED)
            0,7 -> ThreeDS(EciResult.REJECTED)
            else -> throw RuntimeException("ECI value not allowed: $value")
        }

    // HELPER:
    //--------------------------------------------------------------------------------------------------------

    sealed class EciOutcome
    data object NoThreeDS: EciOutcome()
    data class ThreeDS(val outcome: EciResult): EciOutcome()

    enum class EciResult
    {
        SUCCESSFUL,
        ATTEMPTED,
        REJECTED
    }
}
