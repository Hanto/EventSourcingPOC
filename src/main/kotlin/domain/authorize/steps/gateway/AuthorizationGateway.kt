package domain.authorize.steps.gateway

import domain.payment.Payment

interface AuthorizationGateway
{
    fun authorize(payment: Payment): AuthorizeResponse
    fun confirm(payment: Payment): AuthorizeResponse
}
