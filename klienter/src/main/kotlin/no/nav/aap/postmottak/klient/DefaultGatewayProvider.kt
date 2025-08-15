package no.nav.aap.postmottak.klient

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.klient.arena.VeilarbarenaKlient
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytKlient
import no.nav.aap.postmottak.klient.gosysoppgave.GosysOppgaveKlient
import no.nav.aap.postmottak.klient.nom.NomKlient
import no.nav.aap.postmottak.klient.norg.NorgKlient
import no.nav.aap.postmottak.klient.oppgave.OppgaveKlient
import no.nav.aap.postmottak.klient.pdl.PdlGraphqlKlient
import no.nav.aap.postmottak.klient.saf.SafOboRestClient
import no.nav.aap.postmottak.klient.saf.SafRestClient
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlClientCredentialsClient
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlOboClient
import no.nav.aap.postmottak.klient.statistikk.StatistikkKlient
import no.nav.aap.postmottak.klient.unleash.UnleashService

fun createGatewayProvider(body: GatewayRegistry.() -> Unit): GatewayProvider {
    return GatewayProvider(GatewayRegistry().apply(body))
}

fun defaultGatewayProvider(utvidelser: GatewayRegistry.() -> Unit = {}) = createGatewayProvider {
    register<OppgaveKlient>()
    register<GosysOppgaveKlient>()
    register<SafGraphqlClientCredentialsClient>()
    register<SafGraphqlOboClient>()
    register<SafOboRestClient>()
    register<SafRestClient>()
    register<BehandlingsflytKlient>()
    register<NomKlient>()
    register<ArenaKlient>()
    register<PdlGraphqlKlient>()
    register<NorgKlient>()
    register<AapInternApiKlient>()
    register<StatistikkKlient>()
    register<VeilarbarenaKlient>()
    register<UnleashService>()
    utvidelser()
}