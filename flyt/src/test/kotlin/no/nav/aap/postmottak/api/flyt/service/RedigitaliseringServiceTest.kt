package no.nav.aap.postmottak.api.flyt.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import no.nav.aap.postmottak.test.Fakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Fakes
class RedigitaliseringServiceTest {

    private val journalpostId = JournalpostId(123L)
    private val saksnummer = "123456789"
    private val behandlingId = BehandlingId(1L)

    private val journalpost = Journalpost(
        journalpostId = journalpostId,
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
        redigitalisert = false,
    )

    private val journalpostAlreadyRedigitalisert = journalpost.copy(redigitalisert = true)

    private val flytJobbRepository = mockk<FlytJobbRepository>(relaxed = true)
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val journalpostRepository = mockk<JournalpostRepository>(relaxed = true)
    private val saksnummerRepository = mockk<SaksnummerRepository>(relaxed = true)
    private val unleashGateway = mockk<UnleashGateway>(relaxed = true)

    private val service = RedigitaliseringService(
        flytJobbRepository = flytJobbRepository,
        behandlingRepository = behandlingRepository,
        journalpostRepository = journalpostRepository,
        saksnummerRepository = saksnummerRepository,
        unleashGateway = unleashGateway,
    )

    init {
        every { unleashGateway.isEnabled(PostmottakFeature.RedigitaliseringV2) } returns true
    }

    @Test
    fun `returnerer melding hvis journalpost allerede er redigitalisert`() {
        every { journalpostRepository.hentHvisEksisterer(journalpostId) } returns journalpostAlreadyRedigitalisert

        val melding = service.redigitaliser(journalpostId.referanse, saksnummer)

        assertThat(melding).isEqualTo("Journalpost har allerede blitt redigitalisert.")
        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `legger til kopierings-jobb når journalpost ikke er redigitalisert`() {
        every { journalpostRepository.hentHvisEksisterer(journalpostId) } returns journalpost
        every { behandlingRepository.hent(journalpostId) } returns mockk {
            every { id } returns behandlingId
        }
        every { saksnummerRepository.hentSakVurdering(behandlingId) } returns Saksvurdering(saksnummer = saksnummer)

        val melding = service.redigitaliser(journalpostId.referanse, saksnummer)

        assertThat(melding).isNull()
        verify(exactly = 1) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `legger til kopierings-jobb når journalpost ikke finnes lokalt`() {
        every { journalpostRepository.hentHvisEksisterer(journalpostId) } returns null

        val melding = service.redigitaliser(journalpostId.referanse, saksnummer)

        assertThat(melding).isNull()
        verify(exactly = 1) { flytJobbRepository.leggTil(any()) }
        verify(exactly = 0) { behandlingRepository.hent(any<JournalpostId>()) }
        verify(exactly = 0) { saksnummerRepository.hentSakVurdering(any()) }
    }
}
