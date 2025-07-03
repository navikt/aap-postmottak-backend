package no.nav.aap.fordeler.arena

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import no.nav.aap.WithDependencies
import no.nav.aap.fordeler.Diskresjonskode
import no.nav.aap.fordeler.NavEnhet
import no.nav.aap.fordeler.NorgGateway
import no.nav.aap.fordeler.VeilarbarenaGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.EgenAnsattGateway
import no.nav.aap.postmottak.gateway.GeografiskTilknytning
import no.nav.aap.postmottak.gateway.GeografiskTilknytningOgAdressebeskyttelse
import no.nav.aap.postmottak.gateway.Navn
import no.nav.aap.postmottak.gateway.NavnMedIdent
import no.nav.aap.postmottak.gateway.Oppgavetype
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.klient.arena.VeilarbarenaKlient
import no.nav.aap.postmottak.klient.nom.NomKlient
import no.nav.aap.postmottak.klient.norg.NorgKlient
import no.nav.aap.postmottak.klient.pdl.PdlGraphqlKlient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.PERSON_UTEN_SAK_I_BEHANDLINGSFLYT
import no.nav.aap.postmottak.test.fakes.SØKNAD_ETTERSENDELSE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate


@Fakes
class ArenaOppgaveFlytTest : WithDependencies {


    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
        private val motor = Motor(dataSource, 2, jobber = ProsesseringsJobber.alle())

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            GatewayRegistry.register<PdlKlientSpy>()
                .register<ArenaKlientSpy>()
                .register<NomKlientSpy>()
                .register<NorgKlientSpy>()
                .register<VeilarbarenaKlientSpy>()

            motor.start()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            motor.stop()
        }

        private val util =
            TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron() != null }.map { it.type() })
    }

    @Test
    fun `happycase for søknad, oppretter sak i arena og journalfører automatsik`() {
        val journalpostId = PERSON_UTEN_SAK_I_BEHANDLINGSFLYT

        val persondataGateway = GatewayProvider.provide(PersondataGateway::class)
        val arenaGateway = GatewayProvider.provide(ArenaGateway::class)

        every { persondataGateway.hentFødselsdato(any()) } returns LocalDate.now().minusYears(70)
        every { arenaGateway.harAktivSak(any()) } returns false

        dataSource.transaction {
            FlytJobbRepository(it).leggTil(
                JobbInput(FordelingRegelJobbUtfører).forSak(1).medJournalpostId(journalpostId)
            )
        }

        util.ventPåSvar()
        verify(exactly = 1) {
            arenaGateway.opprettArenaOppgave(withArg {
                assertThat(it.oppgaveType).isEqualTo(ArenaOppgaveType.STARTVEDTAK)
            })
        }
    }

    @Test
    fun `happycase for søknad oppretter sak i arena og journalfører automatsik`() {
        val journalpostId = SØKNAD_ETTERSENDELSE

        val persondataGateway = GatewayProvider.provide(PersondataGateway::class)
        val arenaGateway = GatewayProvider.provide(ArenaGateway::class)

        every { persondataGateway.hentFødselsdato(any()) } returns LocalDate.now().minusYears(70)
        every { arenaGateway.harAktivSak(any()) } returns false

        dataSource.transaction {
            FlytJobbRepository(it).leggTil(
                JobbInput(FordelingRegelJobbUtfører).forSak(1).medJournalpostId(journalpostId)
            )
        }
        util.ventPåSvar()
        verify(exactly = 1) {
            arenaGateway.opprettArenaOppgave(withArg {
                assertThat(it.oppgaveType).isEqualTo(ArenaOppgaveType.BEHENVPERSON)
            })
        }
    }

}

class PdlKlientSpy : PersondataGateway {

    companion object : Factory<PdlGraphqlKlient> {
        val klient = spyk(PdlGraphqlKlient.konstruer())
        override fun konstruer() = klient
    }

    override fun hentPersonBolk(personidenter: List<String>): Map<String, NavnMedIdent>? {
        TODO("Not yet implemented")
    }

    override fun hentFødselsdato(personident: String): LocalDate? {
        TODO("Not yet implemented")
    }

    override fun hentGeografiskTilknytning(personident: String): GeografiskTilknytning? {
        TODO("Not yet implemented")
    }

    override fun hentAlleIdenterForPerson(ident: String): List<Ident> {
        TODO("Not yet implemented")
    }

    override fun hentAdressebeskyttelseOgGeolokasjon(ident: Ident): GeografiskTilknytningOgAdressebeskyttelse {
        TODO("Not yet implemented")
    }

    override fun hentNavn(personident: String): Navn? {
        TODO("Not yet implemented")
    }
}

class ArenaKlientSpy : ArenaGateway {

    companion object : Factory<ArenaKlient> {
        val klient = spyk(ArenaKlient.konstruer())
        override fun konstruer() = klient
    }

    override fun nyesteAktiveSak(ident: Ident): String? {
        TODO("Not yet implemented")
    }

    override fun harAktivSak(ident: Ident): Boolean {
        TODO("Not yet implemented")
    }

    override fun opprettArenaOppgave(arenaOpprettetForespørsel: ArenaOpprettOppgaveForespørsel): ArenaOpprettOppgaveRespons {
        TODO("Not yet implemented")
    }

    override fun behandleKjoerelisteOgOpprettOppgave(journalpostId: JournalpostId): String {
        TODO("Not yet implemented")
    }

}

class NomKlientSpy : EgenAnsattGateway {
    companion object : Factory<NomKlient> {
        val klient = spyk(NomKlient.konstruer())
        override fun konstruer() = klient
    }

    override fun erEgenAnsatt(ident: Ident): Boolean {
        TODO("Not yet implemented")
    }
}

class NorgKlientSpy : NorgGateway {
    companion object : Factory<NorgGateway> {
        val klient = spyk(NorgKlient.konstruer())
        override fun konstruer() = klient
    }

    override fun hentAktiveEnheter(): List<NavEnhet> {
        TODO("Not yet implemented")
    }

    override fun finnArbeidsfordelingsEnhet(
        geografiskTilknytning: String?,
        erNavansatt: Boolean,
        diskresjonskode: Diskresjonskode,
        behandlingstema: String,
        behandlingstype: String?,
        oppgavetype: Oppgavetype?
    ): NavEnhet? {
        TODO("Not yet implemented")
    }
}

class VeilarbarenaKlientSpy : VeilarbarenaGateway {
    companion object : Factory<VeilarbarenaKlient> {
        val klient = spyk(VeilarbarenaKlient.konstruer())
        override fun konstruer() = klient
    }

    override fun hentOppfølgingsenhet(personident: String): NavEnhet? {
        TODO("Not yet implemented")
    }
}
