package domain.payment.data

import domain.services.gateway.AuthorizeResponse

sealed class AuthorizeOutcome
{
    data object Skipped: AuthorizeOutcome()
    data class Performed(val authorizeResponse: AuthorizeResponse): AuthorizeOutcome()
}
