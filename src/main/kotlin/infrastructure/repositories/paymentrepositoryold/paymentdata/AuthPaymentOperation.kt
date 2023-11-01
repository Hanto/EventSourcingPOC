package infrastructure.repositories.paymentrepositoryold.paymentdata

import domain.payment.data.paymentaccount.PaymentAccount
import infrastructure.repositories.paymentrepositoryold.TransactionType

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
    val status: Status,
    val paymentClassName: String,
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
