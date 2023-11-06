package domain.payment.data

import domain.services.gateway.AuthenticateResponse

sealed class AuthenticateOutcome
{
    data object Skipped: AuthenticateOutcome()
    data class Performed(val authenticateResponse: AuthenticateResponse): AuthenticateOutcome()
}
