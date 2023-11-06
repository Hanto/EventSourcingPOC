package domain.payment.data.paymentaccount

sealed class AuthorisationAction
{
    data object Moto : AuthorisationAction()
    data class Ecommerce(val authorizationPreference: AuthorizationPreference) : AuthorisationAction()
    data class ThreeDS(val authorizationPreference: AuthorizationPreference) : AuthorisationAction()

    enum class AuthorizationPreference(val label: String) {
        NO_PREFERENCE("No preference"),
        ECI_CHECK("ECI check"),
        TRA_EXEMPTION("TRA exemption"),
    }
}
