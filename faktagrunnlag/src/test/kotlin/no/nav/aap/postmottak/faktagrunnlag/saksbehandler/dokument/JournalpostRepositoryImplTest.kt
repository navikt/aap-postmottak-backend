package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Dokument
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Filtype
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Ident
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.JournalpostStatus
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Variantformat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.dokument.DokumentInfoId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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

            val journalpost = genererJournalpost()
            val behandlingid = behandlingRepository.opprettBehandling(journalpost.journalpostId)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            // Act
            journalpostRepository.lagre(journalpost, behandlingid)

            val hentetJournalpost = journalpostRepository.hentHvisEksisterer(behandlingid)

            assertThat(hentetJournalpost).isEqualTo(journalpost)
            assertThat(hentetJournalpost?.dokumenter()?.size).isGreaterThan(0)
            assertThat(hentetJournalpost?.erSøknad()).isTrue()
            assertThat(hentetJournalpost?.erDigital()).isTrue()
        }
    }

    @Test
    fun `Kan lagre og hente journalpost med lange dokumenttittler`() {
        InitTestDatabase.dataSource.transaction { connection ->
            // Setup
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val journalpost = genererJournalpost(
                listOf(
                    Dokument(
                        tittel = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages",
                        brevkode = "NAV 11-13.05",
                        filtype = Filtype.JSON,
                        variantFormat = Variantformat.ORIGINAL,
                        dokumentInfoId = DokumentInfoId("1")
                    ),
                )
            )
            val behandlingid = behandlingRepository.opprettBehandling(journalpost.journalpostId)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            // Act
            journalpostRepository.lagre(journalpost, behandlingid)

            val hentetJournalpost = journalpostRepository.hentHvisEksisterer(behandlingid)

            assertThat(hentetJournalpost).isEqualTo(journalpost)
            assertThat(hentetJournalpost?.dokumenter()?.size).isGreaterThan(0)
            assertThat(hentetJournalpost?.erSøknad()).isTrue()
            assertThat(hentetJournalpost?.erDigital()).isTrue()
        }
    }

    private fun genererJournalpost(
        dokumenter: List<Dokument>? = null
    ) = Journalpost.MedIdent(
        personident = Ident.Personident("1123123"),
        journalpostId = JournalpostId(10),
        status = JournalpostStatus.MOTTATT,
        mottattDato = LocalDate.of(2021, 1, 1),
        journalførendeEnhet = "YOLO",
        dokumenter = dokumenter ?: listOf(
            Dokument(
                tittel = "Søknad",
                brevkode = "NAV 11-13.05",
                filtype = Filtype.JSON,
                variantFormat = Variantformat.ORIGINAL,
                dokumentInfoId = DokumentInfoId("1")
            ),
            Dokument(
                tittel = "Søknad",
                brevkode = "NAV 11-13.05",
                filtype = Filtype.PDF,
                variantFormat = Variantformat.SLADDET,
                dokumentInfoId = DokumentInfoId("1")
            )
        )
    )
}