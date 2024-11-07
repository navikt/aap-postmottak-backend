package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.sakogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.sakogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.sakogbehandling.journalpost.JournalpostStatus
import no.nav.aap.postmottak.sakogbehandling.journalpost.Person
import no.nav.aap.postmottak.sakogbehandling.journalpost.Variantformat
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class JournalpostRepositoryImplTest {

    @AfterEach
    fun afterEach() {
        InitTestDatabase.dataSource.transaction { it.execute("TRUNCATE BEHANDLING CASCADE") }
    }

    @Test
    fun `Kan lagre og hente journalpost`() {
        InitTestDatabase.dataSource.transaction { connection ->
            // Setup
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val person = PersonRepository(connection).finnEllerOpprett(listOf(Ident("12345678")))
            val journalpost = genererJournalpost(person)
            val behandlingid = behandlingRepository.opprettBehandling(journalpost.journalpostId, TypeBehandling.Journalføring)
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            
            // Act
            journalpostRepository.lagre(journalpost)

            val hentetJournalpost = journalpostRepository.hentHvisEksisterer(behandlingid)

            assertThat(hentetJournalpost).isEqualTo(journalpost)
            assertThat(hentetJournalpost?.dokumenter()?.size).isGreaterThan(0)
            assertThat(hentetJournalpost?.erSøknad()).isTrue()
            assertThat(hentetJournalpost?.erDigital()).isTrue()
        }
    }

    @Test
    fun `Henter nyeste journalpost når det er flere på en behandling`() {
        val behandlingid = InitTestDatabase.dataSource.transaction { connection ->
            // Setup
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val person = PersonRepository(connection).finnEllerOpprett(listOf(Ident("12345678")))
            val journalpost = genererJournalpost(person)
            val behandlingid = behandlingRepository.opprettBehandling(journalpost.journalpostId, TypeBehandling.Journalføring)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            // Act
            journalpostRepository.lagre(journalpost)

            behandlingid
        }
        InitTestDatabase.dataSource.transaction { connection ->
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            val person = PersonRepository(connection).finnEllerOpprett(listOf(Ident("12345678")))
            journalpostRepository.lagre(genererJournalpost(person, tema = "YOLO"))

            val hentetJournalpost = journalpostRepository.hentHvisEksisterer(behandlingid)

            assertThat(hentetJournalpost?.tema).isEqualTo("YOLO")
        }
    }


    private fun genererJournalpost(
        person: Person,
        dokumenter: List<Dokument>? = null,
        tema: String = "AAP"
    ) = Journalpost(
        journalpostId = JournalpostId(10),
        person = person,
        journalførendeEnhet = "YOLO",
        tema = tema,
        status = JournalpostStatus.MOTTATT,
        mottattDato = LocalDate.of(2021, 1, 1),
        dokumenter = dokumenter ?: listOf(
            Dokument(
                brevkode = "NAV 11-13.05",
                filtype = Filtype.JSON,
                variantFormat = Variantformat.ORIGINAL,
                dokumentInfoId = DokumentInfoId("1")
            ),
            Dokument(
                brevkode = "NAV 11-13.05",
                filtype = Filtype.PDF,
                variantFormat = Variantformat.SLADDET,
                dokumentInfoId = DokumentInfoId("1")
            )
        )
    )
}