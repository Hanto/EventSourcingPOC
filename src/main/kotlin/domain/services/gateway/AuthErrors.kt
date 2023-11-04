package domain.services.gateway

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
