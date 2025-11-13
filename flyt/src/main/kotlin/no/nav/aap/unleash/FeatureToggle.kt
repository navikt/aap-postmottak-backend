package no.nav.aap.unleash

interface FeatureToggle {
    fun key(): String
}

enum class PostmottakFeature : FeatureToggle {
    // Eksempel på feature toggle. Kan fjernes når det legges til nye.
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    DummyFeature,
    LukkPostmottakEndreTemaBehandlinger,
    DeaktiverEnhetsregel
    ;

    override fun key(): String = name
}


