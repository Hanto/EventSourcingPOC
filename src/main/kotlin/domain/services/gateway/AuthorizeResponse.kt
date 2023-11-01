package domain.services.gateway

import domain.payment.data.PSPReference
import domain.payment.data.threedstatus.ThreeDSStatus

sealed interface AuthorizeResponse
{
    val threeDSStatus: ThreeDSStatus
    val pspReference: PSPReference

    data class Success(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference

    ) : AuthorizeResponse

    data class Reject(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference,
        val errorDescription: String,
        val errorCode: String,
        val errorReason: ErrorReason,
        val rejectionUseCase: RejectionUseCase = RejectionUseCase.UNDEFINED

    ) : AuthorizeResponse

    data class Fail(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference,
        val errorDescription: String,
        val timeout: Boolean,
        val exception: Exception

    ) : AuthorizeResponse

    data class ClientActionRequested(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference,
        val clientAction: ClientAction

    ) : AuthorizeResponse
}

data class ClientAction
(
    val type: ActionType,
)

enum class ActionType {
    REDIRECT, FINGERPRINT, CHALLENGE
}

enum class ErrorReason {
    AUTHENTICATION_ERROR, AUTHORIZATION_ERROR
}

enum class RejectionUseCase {
    UNDEFINED, SOFT_DECLINE
}
