package domain.payment.data.threedstatus

data class ThreeDSInformation
(
    val exemptionStatus: ExemptionStatus,
    val version: ThreeDSVersion,
    val eci: ECI,
)
