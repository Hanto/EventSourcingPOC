package domain.payment.data.threedstatus

sealed class ThreeDSStatus
{
    data object NoThreeDS: ThreeDSStatus()

    data class PendingThreeDS(

        val version: ThreeDSVersion

    ): ThreeDSStatus()

    data class ThreeDS(

        val exemptionStatus: ExemptionStatus,
        val version: ThreeDSVersion,
        val eci: ECI,
        val transactionId: ThreeDSTransactionId,
        val cavv: CAVV,

        ): ThreeDSStatus()
}
