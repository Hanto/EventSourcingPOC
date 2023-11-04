package domain.services.gateway

import domain.payment.data.PSPReference

sealed interface GatewayResponse
{
    val pspReference: PSPReference

    sealed interface Success: GatewayResponse

    sealed interface Reject: GatewayResponse
    {
        val errorDescription: String
        val errorCode: String
        val errorReason: ErrorReason
        val rejectionUseCase: RejectionUseCase
    }

    sealed  interface Fail: GatewayResponse
    {
        val errorDescription: String
        val timeout: Boolean
        val exception: Exception
    }

    sealed interface ClientActionRequested: GatewayResponse
    {
        val clientAction: ClientAction
    }
}


