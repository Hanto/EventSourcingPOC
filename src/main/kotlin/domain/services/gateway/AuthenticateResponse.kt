package domain.services.gateway

import domain.payment.data.PSPReference
import domain.payment.data.threedstatus.ExemptionStatus
import domain.payment.data.threedstatus.ThreeDSStatus

sealed interface AuthenticateResponse
{
    val threeDSStatus: ThreeDSStatus
    val exemptionStatus: ExemptionStatus
    val pspReference: PSPReference

    data class AuthenticateSuccess(

        override val threeDSStatus: ThreeDSStatus.ThreeDS,
        override val exemptionStatus: ExemptionStatus,
        override val pspReference: PSPReference,

    ) : AuthenticateResponse, AuthenticateOutcome

    data class AuthenticateAndAuthorizeSuccess(

        override val threeDSStatus: ThreeDSStatus,
        override val exemptionStatus: ExemptionStatus,
        override val pspReference: PSPReference

    ) : AuthenticateResponse, AuthenticateOutcome

    data class AuthenticateClientAction(

        override val threeDSStatus: ThreeDSStatus,
        override val exemptionStatus: ExemptionStatus,
        override val pspReference: PSPReference,
        val clientAction: ClientAction

    ) : AuthenticateResponse, AuthenticateOutcome

    data class AuthenticateReject(

        override val threeDSStatus: ThreeDSStatus,
        override val exemptionStatus: ExemptionStatus,
        override val pspReference: PSPReference,
        val errorDescription: String,
        val errorCode: String,
        val errorReason: ErrorReason = ErrorReason.AUTHENTICATION_ERROR,
        val rejectionUseCase: RejectionUseCase = RejectionUseCase.UNDEFINED

    ) : AuthenticateResponse, AuthenticateOutcome

    data class AuthenticateFail(

        override val threeDSStatus: ThreeDSStatus,
        override val exemptionStatus: ExemptionStatus,
        override val pspReference: PSPReference,
        val errorDescription: String,
        val timeout: Boolean,
        val exception: Exception

    ) : AuthenticateResponse, AuthenticateOutcome
}
