package domain.services.gateway

import domain.payment.data.PSPReference
import domain.payment.data.threedstatus.ExemptionStatus

sealed interface AuthorizeResponse
{
    val exemptionStatus: ExemptionStatus
    val pspReference: PSPReference

    data class AuthorizeSuccess(

        override val exemptionStatus: ExemptionStatus,
        override val pspReference: PSPReference

    ) : AuthorizeResponse

    data class AuthorizeReject(

        override val exemptionStatus: ExemptionStatus,
        override val pspReference: PSPReference,
        val errorDescription: String,
        val errorCode: String,
        val errorReason: ErrorReason = ErrorReason.AUTHORIZATION_ERROR,
        val rejectionUseCase: RejectionUseCase = RejectionUseCase.UNDEFINED

    ) : AuthorizeResponse

    data class AuthorizeFail(

        override val exemptionStatus: ExemptionStatus,
        override val pspReference: PSPReference,
        val errorDescription: String,
        val timeout: Boolean,
        val exception: Exception

    ) : AuthorizeResponse
}
