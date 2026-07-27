package no.nav.aap.postmottak.test

import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.postmottak.klient.createGatewayProvider
import no.nav.aap.unleash.UnleashGateway
import kotlin.reflect.KClass

fun fakeGatewayProvider(
    unleashGateway: KClass<out UnleashGateway> = FakeUnleash::class,
    extensions: GatewayRegistry.() -> Unit = {},
) =
    createGatewayProvider {
        register<FakeArenaoppslagGateway>()
        register<FakeJournalpostGateway>()
        register(unleashGateway)
        extensions()
    }