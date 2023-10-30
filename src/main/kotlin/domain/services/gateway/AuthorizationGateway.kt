package domain.services.gateway

import domain.payment.lifecycle.status.ReadyForAuthorization
import domain.payment.lifecycle.status.ReadyForConfirm

interface AuthorizationGateway
{
    fun authorize(payment: ReadyForAuthorization): AuthorizeResponse
    fun confirm(payment: ReadyForConfirm): AuthorizeResponse
}
