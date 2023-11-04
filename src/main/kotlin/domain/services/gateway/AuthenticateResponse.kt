package domain.services.gateway

import domain.payment.data.PSPReference
import domain.payment.data.threedstatus.ThreeDSStatus

sealed interface AuthenticateResponse : GatewayResponse
{
    override val threeDSStatus: ThreeDSStatus
    override val pspReference: PSPReference

    data class AuthenticateSuccess(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference

    ) : AuthenticateResponse, GatewayResponse.Success

    data class AuthenticateReject(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference,
        override val errorDescription: String,
        override val errorCode: String,
        override val errorReason: ErrorReason = ErrorReason.AUTHENTICATION_ERROR,
        override val rejectionUseCase: RejectionUseCase = RejectionUseCase.UNDEFINED

    ) : AuthenticateResponse, GatewayResponse.Reject

    data class AuthenticateFail(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference,
        override val errorDescription: String,
        override val timeout: Boolean,
        override val exception: Exception

    ) : AuthenticateResponse, GatewayResponse.Fail

    data class AuthenticateClientAction(

        override val threeDSStatus: ThreeDSStatus,
        override val pspReference: PSPReference,
        override val clientAction: ClientAction

    ) : AuthenticateResponse, GatewayResponse.ClientActionRequested
}
