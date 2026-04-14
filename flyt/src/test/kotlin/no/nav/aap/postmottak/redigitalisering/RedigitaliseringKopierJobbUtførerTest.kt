package no.nav.aap.postmottak.redigitalisering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.JournalføringService
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.postmottak.prosessering.medSaksnummer
import no.nav.aap.postmottak.test.Fakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Fakes
class RedigitaliseringKopierJobbUtførerTest {

    private val kildeJournalpostId = JournalpostId(123L)
    private val nyJournalpostId = JournalpostId(456L)
    private val saksnummer = "123456789"

    private val eksisterendeJournalpost = Journalpost(
        journalpostId = kildeJournalpostId,
        person = Person(1L, UUID.randomUUID(), listOf(Ident("12345678901"))),
        journalførendeEnhet = null,
        tema = "AAP",
        behandlingstema = null,
        tittel = "Tittel",
        status = Journalstatus.MOTTATT,
        mottattDato = LocalDate.of(2024, 1, 1),
        mottattTid = LocalDateTime.of(2024, 1, 1, 12, 0),
        avsenderMottaker = null,
        dokumenter = listOf(
            Dokument(
                dokumentInfoId = DokumentInfoId("dok1"),
                brevkode = "NAV 11-13.05",
                tittel = "Søknad",
                varianter = listOf(Variant(Filtype.PDF, Variantformat.ARKIV)),
            )
        ),
        kanal = KanalFraKodeverk.SKAN_IM,
        saksnummer = null,
        fagsystem = null,
    )

    private val journalpostRepository = mockk<JournalpostRepository>(relaxed = true)
    private val journalføringService = mockk<JournalføringService>(relaxed = true)
    private val flytJobbRepository = mockk<FlytJobbRepository>(relaxed = true)

    private val jobb = RedigitaliseringKopierJobbUtfører(
        journalpostRepository = journalpostRepository,
        journalføringService = journalføringService,
        flytJobbRepository = flytJobbRepository,
    )

    @Test
    fun `kopierer journalpost og lagrer lokalt med ny id`() {
        every { journalpostRepository.hentHvisEksisterer(kildeJournalpostId) } returns eksisterendeJournalpost
        every { journalføringService.kopierJournalpost(kildeJournalpostId, any()) } returns nyJournalpostId

        jobb.utfør(lagJobbInput())

        verify(exactly = 1) { journalpostRepository.lagre(withArg { assertThat(it.journalpostId).isEqualTo(nyJournalpostId) }) }
    }

    @Test
    fun `legger til RedigitaliseringBehandlingJobb med ny journalpostId og saksnummer`() {
        every { journalpostRepository.hentHvisEksisterer(kildeJournalpostId) } returns eksisterendeJournalpost
        every { journalføringService.kopierJournalpost(kildeJournalpostId, any()) } returns nyJournalpostId

        jobb.utfør(lagJobbInput())

        verify(exactly = 1) {
            flytJobbRepository.leggTil(withArg { input ->
                assertThat(input.type()).isEqualTo(RedigitaliseringBehandlingJobbUtfører.type)
                assertThat(input.sakId()).isEqualTo(nyJournalpostId.referanse)
            })
        }
    }

    private fun lagJobbInput() = JobbInput(RedigitaliseringKopierJobbUtfører)
        .forSak(kildeJournalpostId.referanse)
        .medJournalpostId(kildeJournalpostId)
        .medSaksnummer(saksnummer)
        .medCallId()
}
