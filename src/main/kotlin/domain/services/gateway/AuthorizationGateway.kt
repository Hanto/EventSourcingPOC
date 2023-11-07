package domain.services.gateway

import domain.payment.state.ReadyForAuth
import domain.payment.state.ReadyForAuthenticationAndAuthorizeContinuation
import domain.payment.state.ReadyForAuthenticationContinuation
import domain.payment.state.ReadyForAuthorization

interface AuthorizationGateway
{
    fun authenticate(payment: ReadyForAuth): AuthenticateResponse
    fun continueAuthenticate(payment: ReadyForAuthenticationContinuation): AuthenticateResponse
    fun authorize(payment: ReadyForAuthorization): AuthorizeResponse

    fun authenticateAndAuthorize(payment: ReadyForAuth): AuthenticateResponse
    fun continueAuthenticateAndAuthorize(payment: ReadyForAuthenticationAndAuthorizeContinuation): AuthenticateResponse
}
