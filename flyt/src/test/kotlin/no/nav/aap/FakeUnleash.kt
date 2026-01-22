package no.nav.aap

import no.nav.aap.unleash.FeatureToggle
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway

object FakeUnleash : UnleashGateway {
    override fun isEnabled(featureToggle: FeatureToggle): Boolean {
        check(featureToggle is PostmottakFeature)

        return when (featureToggle) {
            PostmottakFeature.DummyFeature -> TODO()
            PostmottakFeature.SignifikantHistorikkFraArena -> true
        }
    }
}
