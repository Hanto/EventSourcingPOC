package domain.payment.data.paymentaccount

sealed class AuthorisationAction
{
    data object Moto : AuthorisationAction()

    sealed interface NoMoto
    {
        val exemptionPreference: ExemptionPreference
        val authorizationPreference: AuthorizationPreference
    }

    data class Ecommerce(

        override val exemptionPreference: ExemptionPreference,
        override val authorizationPreference: AuthorizationPreference

    ) : AuthorisationAction(), NoMoto

    data class ThreeDS(

        override val exemptionPreference: ExemptionPreference,
        override val authorizationPreference: AuthorizationPreference

    ) : AuthorisationAction(), NoMoto

    enum class ExemptionPreference
    {
        TRY_EXEMPTION,
        DONT_TRY_EXEMPTION
    }

    enum class AuthorizationPreference(val label: String)
    {
        NO_PREFERENCE("No preference"), // only valid for ThreeDS
        ECI_CHECK("ECI check"),         // only valid for ThreeDS
    }
}
