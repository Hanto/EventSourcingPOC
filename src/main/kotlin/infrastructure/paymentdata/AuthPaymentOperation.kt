package infrastructure.paymentdata

import domain.services.routing.PaymentAccount

data class AuthPaymentOperation
(
    val paymentAccount: PaymentAccount?,
    val pspReference: String?,
    val reference: String,
    val retry: Boolean,
    val eci: String?,
    val exemption: Exemption,
    val authenticationStatus: AuthenticationStatus,
    val transactionType: TransactionType,

    val status: Status
)
{
    enum class Status
    {
        OK,
        KO,
        PENDING,
        TIMEOUT
    }

    enum class AuthenticationStatus
    {
        PENDING,
        COMPLETED,
        NOT_APPLICABLE
    }

    sealed class Exemption {

        data object Accepted : Exemption()
        data object NotAccepted: Exemption()
        data object NotRequested: Exemption()
    }
}
