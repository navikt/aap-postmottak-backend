package no.nav.aap.postmottak.under18

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.test.FakeUnleash
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.modell.TestPersoner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@Fakes
class Under18JournalposterInformasjonskravTest {
    private val gatewayProvider = defaultGatewayProvider { register<FakeUnleash>() }

    @Test
    fun `ny kandidat siden forrige kjøring gir endring selv om en annen behandling har lagret journalposten`() {
        TestDataSource().use { dataSource ->
            val person = TestPersoner.leggTil {}
            val hovedjournalpost = TestJournalposter.leggTil { this.person = person }
            val kandidat = TestJournalposter.leggTil { this.person = person; papirsøknad() }
            val oppsett = Under18TestOppsett(dataSource, gatewayProvider)
            val kontekst = oppsett.opprettBehandling(hovedjournalpost)
            assertThat(oppsett.oppdater(kontekst, Under18JournalposterInformasjonskrav)).isEqualTo(IKKE_ENDRET)

            oppsett.registrerMottak(kandidat)
            dataSource.transaction { connection ->
                val provider = oppsett.repositoryRegistry.provider(connection)
                val journalpost = JournalpostService.konstruer(provider, gatewayProvider)
                    .hentJournalpost(kandidat.journalpostId())
                provider.provide<JournalpostRepository>().lagre(journalpost)
            }

            assertThat(oppsett.oppdater(kontekst, Under18JournalposterInformasjonskrav)).isEqualTo(ENDRET)
            assertThat(oppsett.oppdater(kontekst, Under18JournalposterInformasjonskrav)).isEqualTo(IKKE_ENDRET)
        }
    }

    @Test
    fun `nye kandidater gir endring og kjente kandidater gir uendret resultat`() {
        TestDataSource().use { dataSource ->
            val person = TestPersoner.leggTil {}
            val hovedjournalpost = TestJournalposter.leggTil { this.person = person }
            val førsteKandidat = TestJournalposter.leggTil { this.person = person }
            val andreKandidat = TestJournalposter.leggTil { this.person = person }
            val oppsett = Under18TestOppsett(dataSource, gatewayProvider)
            val kontekst = oppsett.opprettBehandling(hovedjournalpost)
            oppsett.registrerMottak(førsteKandidat)

            assertThat(oppsett.oppdater(kontekst, Under18JournalposterInformasjonskrav)).isEqualTo(ENDRET)
            assertThat(oppsett.oppdater(kontekst, Under18JournalposterInformasjonskrav)).isEqualTo(IKKE_ENDRET)

            oppsett.registrerMottak(andreKandidat)
            assertThat(oppsett.oppdater(kontekst, Under18JournalposterInformasjonskrav)).isEqualTo(ENDRET)
            assertThat(oppsett.oppdater(kontekst, Under18JournalposterInformasjonskrav)).isEqualTo(IKKE_ENDRET)

            TestJournalposter.oppdater(førsteKandidat.copy(tema = "DAG"))
            assertThat(oppsett.oppdater(kontekst, Under18JournalposterInformasjonskrav)).isEqualTo(IKKE_ENDRET)

            dataSource.transaction(readOnly = true) { connection ->
                val repository = oppsett.repositoryRegistry.provider(connection).provide<JournalpostRepository>()
                listOf(førsteKandidat, andreKandidat).forEach { kandidat ->
                    assertThat(repository.hentHvisEksisterer(kandidat.journalpostId())).isNull()
                }
            }
        }
    }
}
