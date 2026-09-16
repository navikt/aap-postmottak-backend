package no.nav.aap.postmottak.test.modell

data class MockUnleashFeatures(
    val version: Int = 2,
    val features: List<MockUnleashFeature>,
)

data class MockUnleashFeature(
    val name: String,
    val enabled: Boolean,
)
