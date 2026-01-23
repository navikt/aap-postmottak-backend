package no.nav.aap.unleash

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person

interface UnleashGateway : Gateway {
    fun isEnabled(featureToggle: FeatureToggle): Boolean
    fun isEnabled(featureToggle: FeatureToggle, person: Person): Boolean
}
