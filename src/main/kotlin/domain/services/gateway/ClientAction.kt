package domain.services.gateway

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
