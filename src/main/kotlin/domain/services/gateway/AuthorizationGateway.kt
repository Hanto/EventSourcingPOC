package domain.services.gateway

import domain.payment.state.ReadyForAuth
import domain.payment.state.ReadyForAuthorization
import domain.payment.state.ReadyToContinueAuthentication
import domain.payment.state.ReadyToContinueAuthenticationAndAuthorization

interface AuthorizationGateway
{
    fun authenticate(payment: ReadyForAuth): AuthenticateResponse
    fun continueAuthenticate(payment: ReadyToContinueAuthentication): AuthenticateResponse
    fun authorize(payment: ReadyForAuthorization): AuthorizeResponse

    fun authenticateAndAuthorize(payment: ReadyForAuth): AuthenticateResponse
    fun continueAuthenticateAndAuthorize(payment: ReadyToContinueAuthenticationAndAuthorization): AuthenticateResponse
}
