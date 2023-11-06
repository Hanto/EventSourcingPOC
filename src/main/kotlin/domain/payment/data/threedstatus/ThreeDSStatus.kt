package domain.payment.data.threedstatus

sealed class ThreeDSStatus
{
    data object NoThreeDS: ThreeDSStatus()

    data class PendingThreeDS(

        val version: ThreeDSVersion

    ): ThreeDSStatus()

    data class ThreeDS(

        val version: ThreeDSVersion,
        val eci: ECI,
        val transactionId: ThreeDSTransactionId, // Mask when persisting
        val cavv: CAVV,                          // Mask when persisting
        val xid: XID,                            // Mask when persisting

    ): ThreeDSStatus()
}
