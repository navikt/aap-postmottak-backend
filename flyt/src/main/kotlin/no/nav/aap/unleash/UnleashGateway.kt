package no.nav.aap.unleash

import no.nav.aap.komponenter.gateway.Gateway

interface UnleashGateway : Gateway {
    fun isEnabled(featureToggle: FeatureToggle): Boolean
    fun isEnabled(featureToggle: FeatureToggle, userId: String): Boolean
}
