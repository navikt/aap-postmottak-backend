package no.nav.aap.postmottak.flyt

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.PersonRepository
import no.nav.aap.postmottak.flyt.flate.visning.DynamiskStegGruppeVisningService
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.sakogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.sakogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.sakogbehandling.journalpost.JournalpostStatus
import no.nav.aap.postmottak.sakogbehandling.journalpost.Variantformat
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DynamiskStegGruppeVisningServiceTest {
    @Test
    fun `Skal vise steg 'overlever til fagsystem' for søknad`() {
        InitTestDatabase.dataSource.transaction { connection ->
            // Arrange
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val person = PersonRepository(connection).finnEllerOpprett(listOf(Ident("12345678")))
            val journalpost = Journalpost(
                person =  person,
                journalpostId = JournalpostId(111),
                journalførendeEnhet = null,
                status = JournalpostStatus.MOTTATT,
                tema = "AAP",
                dokumenter = listOf(
                    Dokument(
                        brevkode = "NAV 11-13.05",
                        filtype = Filtype.JSON,
                        variantFormat = Variantformat.ORIGINAL,
                        dokumentInfoId = DokumentInfoId("1")
                    )
                ),
                mottattDato = LocalDate.of(2021, 1, 1)
            )
            
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