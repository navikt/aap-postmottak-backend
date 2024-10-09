package no.nav.aap.postmottak.flyt

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Dokument
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Filtype
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Ident
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.JournalpostStatus
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Variantformat
import no.nav.aap.postmottak.flyt.flate.visning.DynamiskStegGruppeVisningService
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.dokument.DokumentInfoId
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DynamiskStegGruppeVisningServiceTest {
    val journalpost = Journalpost.MedIdent(
        journalpostId = JournalpostId(111),
        journalførendeEnhet = null,
        status = JournalpostStatus.MOTTATT,
        dokumenter = listOf(
            Dokument(
                brevkode = "NAV 11-13.05",
                filtype = Filtype.JSON,
                variantFormat = Variantformat.ORIGINAL,
                dokumentInfoId = DokumentInfoId("1")
            )
        ),
        personident = Ident.Personident("12345678901"),
        mottattDato = LocalDate.of(2021, 1, 1)

    )

    @Test
    fun `Skal vise steg 'overlever til fagsystem' for søknad`() {
        InitTestDatabase.dataSource.transaction { connection ->
            // Arrange
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val behandlingId = behandlingRepository.opprettBehandling(journalpost.journalpostId)
            journalpostRepository.lagre(journalpost, behandlingId)

            val service = DynamiskStegGruppeVisningService(connection)
            
            val gruppe = StegGruppe.OVERLEVER_TIL_FAGSYSTEM

            // Act
            val result = service.skalVises(gruppe, behandlingId)

            // Assert
            assertTrue(result)
        }
    }
}