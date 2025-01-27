package no.nav.aap

import io.mockk.mockk
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.klient.AapInternApiKlient
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.postmottak.klient.gosysoppgave.GosysOppgaveKlient
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.klient.nom.NomKlient
import no.nav.aap.postmottak.klient.norg.NorgKlient
import no.nav.aap.postmottak.klient.oppgave.OppgaveKlient
import no.nav.aap.postmottak.klient.pdl.PdlGraphqlKlient
import no.nav.aap.postmottak.klient.saf.SafRestClient
import no.nav.aap.postmottak.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.OverleveringVurderingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.StruktureringsvurderingRepositoryImpl
import no.nav.aap.postmottak.repository.fordeler.InnkommendeJournalpostRepositoryImpl
import no.nav.aap.postmottak.repository.fordeler.RegelRepositoryImpl
import no.nav.aap.postmottak.repository.journalpost.JournalpostRepositoryImpl
import no.nav.aap.postmottak.repository.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.postmottak.repository.person.PersonRepositoryImpl
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClientCredentialsClient
import org.junit.jupiter.api.BeforeAll

interface WithDependencies {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            RepositoryRegistry.register<SaksnummerRepositoryImpl>()
                .register<JournalpostRepositoryImpl>()
                .register<TaSkriveLåsRepositoryImpl>()
                .register<AvklaringsbehovRepositoryImpl>()
                .register<BehandlingRepositoryImpl>()
                .register<AvklarTemaRepositoryImpl>()
                .register<AvklarTemaRepositoryImpl>()
                .register<StruktureringsvurderingRepositoryImpl>()
                .register<PersonRepositoryImpl>()
                .register<InnkommendeJournalpostRepositoryImpl>()
                .register<RegelRepositoryImpl>()
                .register<OverleveringVurderingRepositoryImpl>()

            GatewayRegistry
                .register<OppgaveKlient>()
                .register<GosysOppgaveKlient>()
                .register<SafGraphqlClientCredentialsClient>()
                .register<SafRestClient>()
                .register<BehandlingsflytClient>()
                .register<JoarkClient>()
                .register<PdlGraphqlKlient>()
                .register<NorgKlient>()
                .register<NomKlient>()
                .register<ArenaKlient>()
                .register<AapInternApiKlient>()


            PrometheusProvider.prometheus = mockk(relaxed = true)
        }
    }
}