package domain.services.gateway

import domain.payment.data.PSPReference
import domain.payment.data.threedstatus.ThreeDSStatus

sealed interface AuthorizeResponse: GatewayResponse
{
    override val threeDSStatus: ThreeDSStatus
    override val pspReference: PSPReference

    data class AuthorizeSuccess(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference

    ) : AuthorizeResponse, GatewayResponse.Success

    data class AuthorizeReject(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference,
        override val errorDescription: String,
        override val errorCode: String,
        override val errorReason: ErrorReason = ErrorReason.AUTHORIZATION_ERROR,
        override val rejectionUseCase: RejectionUseCase = RejectionUseCase.UNDEFINED

    ) : AuthorizeResponse, GatewayResponse.Reject

    data class AuthorizeFail(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference,
        override val errorDescription: String,
        override val timeout: Boolean,
        override val exception: Exception

    ) : AuthorizeResponse, GatewayResponse.Fail

    data class AuthorizeClientAction(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference,
        override val clientAction: ClientAction

    ) : AuthorizeResponse, GatewayResponse.ClientActionRequested
}

data class ClientAction
(
    val type: ActionType,
)

enum class ActionType
{
    REDIRECT,
    FINGERPRINT,
    CHALLENGE
}

enum class ErrorReason
{
    AUTHENTICATION_ERROR,
    AUTHORIZATION_ERROR
}

enum class RejectionUseCase
{
    UNDEFINED,
    SOFT_DECLINE
}
