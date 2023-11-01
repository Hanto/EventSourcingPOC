package domain.payment.data.threedsinformation

data class ThreeDSInformation
(
    val exemptionStatus: ExemptionStatus,
    val version: ThreeDSVersion,
    val eci: ECI,
)
