package domain.services.gateway

sealed interface AuthorizeOutcome
{
    data object Skipped: AuthorizeOutcome
}
