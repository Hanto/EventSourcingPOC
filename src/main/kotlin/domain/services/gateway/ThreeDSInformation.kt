package domain.services.gateway

data class ThreeDSInformation
(
    val exemptionStatus: ExemptionStatus,
    val version: ThreeDSVersion,
    val eci: ECI,
)
