package domain.payment.data.threedstatus

sealed class ThreeDSStatus
{
    data object PendingThreeDS: ThreeDSStatus()
    data object NoThreeDS: ThreeDSStatus()
    data class ThreeDS(val info: ThreeDSInformation): ThreeDSStatus()
}
