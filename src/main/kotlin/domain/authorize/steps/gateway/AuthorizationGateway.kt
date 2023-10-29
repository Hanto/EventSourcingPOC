package domain.authorize.steps.gateway

import domain.authorize.status.ReadyForAuthorization
import domain.authorize.status.ReadyForConfirm

interface AuthorizationGateway
{
    fun authorize(payment: ReadyForAuthorization): AuthorizeResponse
    fun confirm(payment: ReadyForConfirm): AuthorizeResponse
}
