package domain.lifecycle.steps.gateway

import domain.lifecycle.status.ReadyForAuthorization
import domain.lifecycle.status.ReadyForConfirm

interface AuthorizationGateway
{
    fun authorize(payment: ReadyForAuthorization): AuthorizeResponse
    fun confirm(payment: ReadyForConfirm): AuthorizeResponse
}
