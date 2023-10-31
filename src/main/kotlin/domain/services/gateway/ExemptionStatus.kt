package domain.services.gateway

sealed class ExemptionStatus
{
    data object ExemptionNotRequested: ExemptionStatus()
    data object ExemptionAccepted: ExemptionStatus()
    data object ExemptionNotAccepted: ExemptionStatus()
}
