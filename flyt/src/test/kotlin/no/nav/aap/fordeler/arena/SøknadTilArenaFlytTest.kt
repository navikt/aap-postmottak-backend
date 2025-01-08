package no.nav.aap.fordeler.arena

import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import no.nav.aap.WithDependencies
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.gateway.GeografiskTilknytning
import no.nav.aap.postmottak.gateway.GeografiskTilknytningOgAdressebeskyttelse
import no.nav.aap.postmottak.gateway.Navn
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.klient.pdl.PdlGraphqlKlient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.postmottak.test.WithMotor
import no.nav.aap.postmottak.test.await
import no.nav.aap.postmottak.test.fakes.WithFakes
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate



class SøknadTilArenaFlytTest: WithFakes, WithDependencies, WithMotor {

    companion object {

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            GatewayRegistry.register<PdlKlientSpy>()
                .register<ArenaKlientSpy>()
        }
    }

    @AfterEach
    fun clean() {
        InitTestDatabase.dataSource.transaction {
            it.execute("""TRUNCATE JOBB CASCADE""")
        }
    }

    val dataSource = InitTestDatabase.dataSource

    @Test
    fun `happycase for søknad, oppretter sak i arena og journalfører automatsik`() {
        val journalpostId = JournalpostId(1)

        val persondataGateway = GatewayProvider.provide(PersondataGateway::class)
        val arenaGateway = GatewayProvider.provide(ArenaGateway::class)

        every { persondataGateway.hentFødselsdato(any()) } returns LocalDate.now().minusYears(70)
        every { arenaGateway.harAktivSak(any()) } returns false

        dataSource.transaction {
            FlytJobbRepository(it).leggTil(JobbInput(FordelingRegelJobbUtfører).forSak(1).medJournalpostId(journalpostId))
        }

        await(10000) {
            verify(exactly = 1) { arenaGateway.opprettArenaOppgave(any()) }
        }
    }

}

class PdlKlientSpy: PersondataGateway {

    companion object: Factory<PdlGraphqlKlient> {
        val klient = spyk(PdlGraphqlKlient.konstruer())
        override fun konstruer() = klient
    }

    override fun hentPersonBolk(personidenter: List<String>): Map<String, Navn>? {
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

}

class ArenaKlientSpy: ArenaGateway {

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