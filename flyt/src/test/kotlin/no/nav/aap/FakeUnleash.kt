package no.nav.aap

import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.unleash.FeatureToggle
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway

object FakeUnleash : UnleashGateway {

    override fun isEnabled(featureToggle: FeatureToggle): Boolean {
        check(featureToggle is PostmottakFeature)

        return when (featureToggle) {
            PostmottakFeature.DummyFeature -> TODO()
            PostmottakFeature.AktiverSignifikantArenaHistorikkRegel -> TODO()
            PostmottakFeature.TestAvSignifikantHistorikkFilter -> TODO()
        }
    }

    override fun isEnabled(featureToggle: FeatureToggle, person: Person): Boolean {
        return when (featureToggle) {
            PostmottakFeature.DummyFeature -> isEnabled(featureToggle)
            PostmottakFeature.AktiverSignifikantArenaHistorikkRegel -> isRolledOutFor(person)
            PostmottakFeature.TestAvSignifikantHistorikkFilter -> isRolledOutFor(person)

            else -> false
        }

    }

    val rejectList = mutableSetOf<String>() // for å teste gradual rollout basert på userId
    private fun isRolledOutFor(person: Person): Boolean = !rejectList.contains(person.identifikator.toString())
}

