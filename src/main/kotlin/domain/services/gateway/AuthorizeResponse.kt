package domain.services.gateway

data class AuthorizeResponse
(
    val status: AuthorizeStatus,
    val threeDSInformation: ThreeDSInformation?
)

sealed class AuthorizeStatus {

    data object Success : AuthorizeStatus()
    data class Reject(val errorDescription: String, val errorCode: String?, val errorReason: ErrorReason?, val rejectionUseCase: RejectionUseCase = RejectionUseCase.UNDEFINED) : AuthorizeStatus()
    data class Fail(val errorDescription: String, val timeout: Boolean, val exception: Exception?) : AuthorizeStatus()
    data class ClientActionRequested(val clientAction: ClientAction) : AuthorizeStatus()
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
