package domain.services.gateway

sealed interface AuthorizeResponse
{
    val threeDSStatus: ThreeDSStatus

    data class Success(

        override val threeDSStatus: ThreeDSStatus,

    ) : AuthorizeResponse

    data class Reject(

        override val threeDSStatus: ThreeDSStatus,
        val errorDescription: String,
        val errorCode: String,
        val errorReason: ErrorReason,
        val rejectionUseCase: RejectionUseCase = RejectionUseCase.UNDEFINED

    ) : AuthorizeResponse

    data class Fail(

        override val threeDSStatus: ThreeDSStatus,
        val errorDescription: String,
        val timeout: Boolean,
        val exception: Exception

    ) : AuthorizeResponse

    data class ClientActionRequested(

        override val threeDSStatus: ThreeDSStatus,
        val clientAction: ClientAction

    ) : AuthorizeResponse
}

sealed class ThreeDSStatus
{
    data object PendingThreeDS: ThreeDSStatus()
    data object NoThreeDS: ThreeDSStatus()
    data class ThreeDS(val info: ThreeDSInformation): ThreeDSStatus()
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
