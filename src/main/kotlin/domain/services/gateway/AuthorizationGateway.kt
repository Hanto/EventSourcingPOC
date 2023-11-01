package domain.services.gateway

import domain.payment.state.ReadyForAuthorization
import domain.payment.state.ReadyForConfirm

interface AuthorizationGateway
{
    fun authorize(payment: ReadyForAuthorization): AuthorizeResponse
    fun confirm(payment: ReadyForConfirm): AuthorizeResponse
}
