package no.nav.aap.postmottak.klient.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.unleash.FeatureToggle
import no.nav.aap.unleash.UnleashGateway

object UnleashService : UnleashGateway {
    private val unleash = DefaultUnleash(
        UnleashConfig
            .builder()
            .appName(requiredConfigForKey("nais.app.name"))
            .unleashAPI("${requiredConfigForKey("unleash.server.api.url")}/api")
            .apiKey(requiredConfigForKey("unleash.server.api.token"))
            .build()
    )

    override fun isEnabled(featureToggle: FeatureToggle): Boolean = unleash.isEnabled(featureToggle.key())

    override fun isEnabled(featureToggle: FeatureToggle, userId: String): Boolean {
        val context = UnleashContext.builder().userId(userId).build()
        return unleash.isEnabled(featureToggle.key(), context)
    }

}
