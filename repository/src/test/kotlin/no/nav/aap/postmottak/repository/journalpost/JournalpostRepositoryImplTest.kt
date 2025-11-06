package no.nav.aap.postmottak.repository.journalpost

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.AvsenderMottaker
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.person.PersonRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class JournalpostRepositoryImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun beforeEach() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `Kan lagre og hente journalpost`() {
        dataSource.transaction { connection ->
            // Setup
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("12345678")))
            val journalpost = genererJournalpost(person)
            val behandlingid =
                behandlingRepository.opprettBehandling(journalpost.journalpostId, TypeBehandling.Journalføring)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            // Act
            journalpostRepository.lagre(journalpost)

            val hentetJournalpost = journalpostRepository.hentHvisEksisterer(behandlingid)

            assertThat(hentetJournalpost).usingRecursiveComparison().isEqualTo(journalpost)
            assertThat(hentetJournalpost?.dokumenter?.size).isGreaterThan(0)
            assertThat(hentetJournalpost?.erSøknad()).isTrue()
            assertThat(hentetJournalpost?.erDigital()).isTrue()
        }
    }

    @Test
    fun `Henter nyeste journalpost når det er flere på en behandling`() {
        val behandlingid = dataSource.transaction { connection ->
            // Setup
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("12345678")))
            val journalpost = genererJournalpost(person)
            val behandlingid =
                behandlingRepository.opprettBehandling(journalpost.journalpostId, TypeBehandling.Journalføring)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            // Act
            journalpostRepository.lagre(journalpost)

            behandlingid
        }
        dataSource.transaction { connection ->
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("12345678")))
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
        behandlingstema = null,
        tittel = "Tittel på journalposten",
        status = Journalstatus.MOTTATT,
        mottattDato = LocalDate.of(2021, 1, 1),
        mottattTid = LocalDate.of(2021, 1, 1).atStartOfDay(),
        kanal = KanalFraKodeverk.UKJENT,
        saksnummer = "saksnummer",
        fagsystem = Fagsystem.KELVIN.name,
        avsenderMottaker = AvsenderMottaker("id", "FNR", "Navn"),
        dokumenter = dokumenter ?: listOf(
            Dokument(
                brevkode = "NAV 11-13.05",
                dokumentInfoId = DokumentInfoId("1"),
                tittel = "Søknad om AAP",
                varianter = listOf(
                    Variant(
                        filtype = Filtype.JSON,
                        variantformat = Variantformat.ORIGINAL
                    )
                ),
            ),
            Dokument(
                dokumentInfoId = DokumentInfoId("2"),
                brevkode = "NAV 11-13.05",
                tittel = "Vedlegg til søknad",
                varianter = listOf(
                    Variant(
                        filtype = Filtype.JSON,
                        variantformat = Variantformat.ORIGINAL
                    ),
                    Variant(
                        filtype = Filtype.PDF,
                        variantformat = Variantformat.SLADDET
                    )
                ),
            )
        )
    )
}