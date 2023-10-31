package domain.services.gateway

sealed class ExemptionStatus
{
    data object ExemptionNotRequested: ExemptionStatus()
    data class ExemptionRequested(val exemptionAccepted: Boolean): ExemptionStatus()
}

