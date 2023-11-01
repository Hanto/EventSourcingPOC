package domain.payment.data.threedsinformation

sealed class ExemptionStatus
{
    data object ExemptionNotRequested: ExemptionStatus()
    data object ExemptionAccepted: ExemptionStatus()
    data object ExemptionNotAccepted: ExemptionStatus()
}
