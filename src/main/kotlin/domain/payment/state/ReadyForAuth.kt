package domain.payment.state

import domain.services.gateway.AuthorizationGateway

sealed interface ReadyForAuth : Payment
{
    fun addAuthenticationResponse(authorizeService: AuthorizationGateway): Payment
}
