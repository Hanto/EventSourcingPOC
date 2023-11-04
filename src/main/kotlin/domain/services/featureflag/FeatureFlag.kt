package domain.services.featureflag

interface FeatureFlag
{
    fun isFeatureEnabledFor(feature: Feature): Boolean

    enum class Feature {
        DECOUPLED_AUTH
    }
}
