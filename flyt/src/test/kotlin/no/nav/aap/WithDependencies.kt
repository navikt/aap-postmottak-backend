package no.nav.aap

import io.mockk.mockk
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.klient.AapInternApiKlient
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.klient.arena.VeilarbarenaKlient
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytKlient
import no.nav.aap.postmottak.klient.gosysoppgave.GosysOppgaveKlient
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.klient.nom.NomKlient
import no.nav.aap.postmottak.klient.norg.NorgKlient
import no.nav.aap.postmottak.klient.oppgave.OppgaveKlient
import no.nav.aap.postmottak.klient.pdl.PdlGraphqlKlient
import no.nav.aap.postmottak.klient.saf.SafRestClient
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlClientCredentialsClient
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlOboClient
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import org.junit.jupiter.api.BeforeAll

interface WithDependencies {
    companion object {
        val repositoryRegistry = postgresRepositoryRegistry
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            GatewayRegistry
                .register<OppgaveKlient>()
                .register<GosysOppgaveKlient>()
                .register<SafGraphqlClientCredentialsClient>()
                .register<SafGraphqlOboClient>()
                .register<SafRestClient>()
                .register<BehandlingsflytKlient>()
                .register<JoarkClient>()
                .register<PdlGraphqlKlient>()
                .register<NorgKlient>()
                .register<NomKlient>()
                .register<ArenaKlient>()
                .register<AapInternApiKlient>()
                .register<FakeStatistikkKlient>()
                .register<VeilarbarenaKlient>()

            PrometheusProvider.prometheus = mockk(relaxed = true)
        }
    }
}