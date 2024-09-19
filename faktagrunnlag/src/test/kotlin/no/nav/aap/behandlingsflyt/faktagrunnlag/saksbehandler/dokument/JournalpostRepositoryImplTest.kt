package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Dokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Filtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.JournalpostStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Variantformat
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.verdityper.dokument.DokumentInfoId
import java.time.LocalDate

class JournalpostRepositoryImplTest {
    @Test
    fun `Kan lagre og hente journalpost`() {
        val behandlingId = InitTestDatabase.dataSource.transaction { connection ->
            // Setup
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val journalpost = genererJournalpost()
            val behandling = behandlingRepository.opprettBehandling(journalpost.journalpostId)
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            
            // Act
            journalpostRepository.lagre(journalpost, behandling.id)

            val hentetJournalpost = journalpostRepository.hentHvisEksisterer(behandling.id)

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
        journalpostId =  JournalpostId(10),
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