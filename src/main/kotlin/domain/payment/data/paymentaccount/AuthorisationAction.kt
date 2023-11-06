package domain.payment.data.paymentaccount

sealed class AuthorisationAction
{
    data object Moto : AuthorisationAction()

    data class Ecommerce(

        val exemptionPreference: ExemptionPreference,
        val authorizationPreference: AuthorizationPreference

    ) : AuthorisationAction()

    data class ThreeDS(

        val exemptionPreference: ExemptionPreference,
        val authorizationPreference: AuthorizationPreference

    ) : AuthorisationAction()

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
