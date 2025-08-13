package no.nav.aap.postmottak.flyt

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.flyt.flate.visning.DynamiskStegGruppeVisningService
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.DigitaliseringsvurderingRepositoryImpl
import no.nav.aap.postmottak.repository.journalpost.JournalpostRepositoryImpl
import no.nav.aap.postmottak.repository.person.PersonRepositoryImpl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DynamiskStegGruppeVisningServiceTest {
    val repositoryRegistry = RepositoryRegistry()
        .register<JournalpostRepositoryImpl>()
        .register<AvklarTemaRepositoryImpl>()
        .register<DigitaliseringsvurderingRepositoryImpl>()
        .register<BehandlingRepositoryImpl>()

    @Test
    fun `Skal vise steg 'overlever til fagsystem' for søknad`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            // Arrange
            val repositoryProvider = repositoryRegistry.provider(connection)
            val journalpostRepository: JournalpostRepository = repositoryProvider.provide()
            val behandlingRepository: BehandlingRepository = repositoryProvider.provide()

            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("12345678")))
            val journalpost = Journalpost(
                person = person,
                journalpostId = JournalpostId(111),
                journalførendeEnhet = null,
                status = Journalstatus.MOTTATT,
                tema = "AAP",
                behandlingstema = null,
                tittel = "Tittel pa journalposten",
                avsenderMottaker = null,
                dokumenter = listOf(
                    Dokument(
                        brevkode = "NAV 11-13.05",
                        dokumentInfoId = DokumentInfoId("1"),
                        varianter = listOf(
                            Variant(
                                filtype = Filtype.JSON,
                                variantformat = Variantformat.ORIGINAL
                            )
                        )
                    )
                ),
                mottattDato = LocalDate.of(2021, 1, 1),
                mottattTid = LocalDate.of(2021, 1, 1).atStartOfDay(),
                kanal = KanalFraKodeverk.UKJENT,
                saksnummer = null,
                fagsystem = null
            )

            val behandlingId =
                behandlingRepository.opprettBehandling(journalpost.journalpostId, TypeBehandling.Journalføring)
            journalpostRepository.lagre(journalpost)

            val service = DynamiskStegGruppeVisningService(repositoryProvider)

            val gruppe = StegGruppe.OVERLEVER_TIL_FAGSYSTEM

            // Act
            val result = service.skalVises(gruppe, behandlingId)

            // Assert
            assertTrue(result)
        }
    }
}