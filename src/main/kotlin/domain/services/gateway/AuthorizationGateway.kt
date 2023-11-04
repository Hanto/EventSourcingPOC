package domain.services.gateway

import domain.payment.state.ReadyForAuthentication
import domain.payment.state.ReadyForAuthenticationConfirm
import domain.payment.state.ReadyForAuthorization

interface AuthorizationGateway
{
    fun authenticate(payment: ReadyForAuthentication): AuthenticateResponse
    fun confirmAuthenticate(payment: ReadyForAuthenticationConfirm): AuthenticateResponse
    fun authorize(payment: ReadyForAuthorization): AuthorizeResponse
}
