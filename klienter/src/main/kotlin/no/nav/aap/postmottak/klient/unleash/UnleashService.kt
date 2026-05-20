package no.nav.aap.postmottak.klient.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.unleash.FeatureToggle
import no.nav.aap.unleash.UnleashGateway

object UnleashService : UnleashGateway {
    private val unleash = DefaultUnleash(
        UnleashConfig
            .builder()
            .appName(requiredConfigForKey("NAIS_APP_NAME"))
            .unleashAPI("${requiredConfigForKey("UNLEASH_SERVER_API_URL")}/api")
            .apiKey(requiredConfigForKey("UNLEASH_SERVER_API_TOKEN"))
            .build()
    )

    override fun isEnabled(featureToggle: FeatureToggle): Boolean = unleash.isEnabled(featureToggle.key())

    override fun isEnabled(featureToggle: FeatureToggle, userId: String): Boolean {
        val context = UnleashContext.builder().userId(userId).build()
        return unleash.isEnabled(featureToggle.key(), context)
    }

}
