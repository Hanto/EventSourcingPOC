package domain.services.gateway

import domain.payment.state.ReadyForAuthentication
import domain.payment.state.ReadyForAuthenticationConfirm
import domain.payment.state.ReadyForAuthorization
import domain.payment.state.ReadyForAuthorizationConfirm

interface AuthorizationGateway
{
    fun authenticate(payment: ReadyForAuthentication): AuthenticateResponse
    fun confirmAuthenticate(payment: ReadyForAuthenticationConfirm): AuthenticateResponse
    fun authorize(payment: ReadyForAuthorization): AuthorizeResponse
    fun confirmAuthorize(payment: ReadyForAuthorizationConfirm): AuthorizeResponse
    fun authenticateAndAuthorize(payment: ReadyForAuthentication): AuthenticateAndAuthorizeResponse
}
