package domain.payment.data.threedsinformation

data class ThreeDSVersion
    (
    val value: String
)
{
    // MAIN:
    //--------------------------------------------------------------------------------------------------------

    fun majorVersion(): Version =

        when
        {
            value.startsWith("1") -> Version.V1
            value.startsWith("2.1") -> Version.V2_1
            value.startsWith("2.2") -> Version.V2_2
            else -> throw RuntimeException("Unsupported 3DS version: $value")
        }

    // HELPER:
    //--------------------------------------------------------------------------------------------------------

    enum class Version
    {
        V1,
        V2_1,
        V2_2
    }
}
