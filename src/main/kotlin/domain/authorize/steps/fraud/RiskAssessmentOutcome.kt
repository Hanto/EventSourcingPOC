package domain.authorize.steps.fraud

enum class RiskAssessmentOutcome
{
    FRICTIONLESS,
    AUTHENTICATION_PREFERRED,
    AUTHENTICATION_MANDATORY
}
