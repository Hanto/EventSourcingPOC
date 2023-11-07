package domain.services.gateway

sealed interface AuthenticateOutcome
{
    data object Skipped: AuthenticateOutcome
}
