package no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.FunnetAvklaringsbehov
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate


class OverleverTilFagsystemStegTest {

    val struktureringsvurderingRepository: DigitaliseringsvurderingRepository = mockk(relaxed = true)
    val behandlingsflytKlient: BehandlingsflytGateway = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepository = mockk()
    val saksnummerRepository: SaksnummerRepository = mockk()
    val overleveringVurderingRepository: OverleveringVurderingRepository = mockk()

    val overførTilFagsystemSteg = OverleverTilFagsystemSteg(
        struktureringsvurderingRepository,
        behandlingsflytKlient,
        journalpostRepository,
        saksnummerRepository,
        overleveringVurderingRepository,
    )

    val kontekst: FlytKontekst = mockk(relaxed = true)
    val journalpostId: JournalpostId = JournalpostId(123)
    val behandling: Behandling = mockk()
    val saksnummer = "String"
    val kanal = KanalFraKodeverk.NAV_NO
    val mottattDato = LocalDate.of(
        2021,
        1,
        1
    )

    @BeforeEach
    fun beforeEach() {
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns
                TestJournalposter.leggTil { journalpostId = 123 }.tilJournalpost()
        every { saksnummerRepository.hentSakVurdering(any())?.saksnummer } returns saksnummer
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }


    @Test
    fun `hvis søknad er manuelt strukturert, blir strukturert dokument sendt til behandlingsflyt`() {
        val kontekst: FlytKontekst = mockk(relaxed = true)
        val struktureringsvurdering = Digitaliseringsvurdering(
            InnsendingType.SØKNAD, """{
            |"yrkesskade": "Nei",
            |"student": {"erStudent":"Nei", "kommeTilbake": "Nei"}
            |}""".trimMargin(), mottattDato, null
        )

        val journalpost = TestJournalposter.leggTil {
            journalpostId = 123
            digitalSøknad()
        }.tilJournalpost()
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { overleveringVurderingRepository.hentHvisEksisterer(any()) } returns null
        every { overleveringVurderingRepository.lagre(any(), any()) } returns Unit
        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns struktureringsvurdering

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 1) {
            behandlingsflytKlient.sendHendelse(
                journalpostId,
                kanal,
                mottattDato.atStartOfDay(),
                InnsendingType.SØKNAD,
                saksnummer,
                DokumentTilMeldingParser
                    .parseTilMelding(struktureringsvurdering.strukturertDokument, InnsendingType.SØKNAD),
                false
            )
        }
    }

    @Test
    fun `hvis automatisk journalføring blir digital søknad fra joark sendt til behandlingsflyt`() {
        val journalpostJson = """{
            |"yrkesskade": "Nei",
            |"student": {"erStudent": "Nei", "kommeTilbake": "Nei"},
            |"oppgitteBarn": {"identer": []}
            |}""".trimMargin()

        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns Digitaliseringsvurdering(
            InnsendingType.SØKNAD, journalpostJson, mottattDato, null
        )
        every { overleveringVurderingRepository.hentHvisEksisterer(any()) } returns null
        every { overleveringVurderingRepository.lagre(any(), any()) } returns Unit

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 1) {
            behandlingsflytKlient.sendHendelse(
                journalpostId,
                kanal,
                mottattDato.atStartOfDay(),
                InnsendingType.SØKNAD,
                saksnummer,
                DokumentTilMeldingParser
                    .parseTilMelding(journalpostJson, InnsendingType.SØKNAD),
                false
            )
        }
    }

    @Test
    fun `hvis journalposten er dialogmelding kreves manuell avklaring`() {
        every { struktureringsvurderingRepository.hentHvisEksisterer(any())?.kategori } returns InnsendingType.DIALOGMELDING
        every { overleveringVurderingRepository.hentHvisEksisterer(any()) } returns null

        val stegresultat = overførTilFagsystemSteg.utfør(kontekst)
        assertEquals(stegresultat::class.simpleName, FantAvklaringsbehov::class.simpleName)
        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).isEqualTo(Definisjon.AVKLAR_OVERLEVERING)
    }

    @Test
    fun `dialogmelding som skal til behandlingsflyt blir sendt korrekt`() {
        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns Digitaliseringsvurdering(
            InnsendingType.DIALOGMELDING,
            null,
            null,
            null
        )
        every { overleveringVurderingRepository.hentHvisEksisterer(any()) } returns OverleveringVurdering(true)
        val journalpost = TestJournalposter.leggTil { journalpostId = 123 }
            .tilJournalpost(mottattTid = mottattDato.atStartOfDay())
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

        val stegresultat = overførTilFagsystemSteg.utfør(kontekst)
        verify(exactly = 1) {
            behandlingsflytKlient.sendHendelse(
                journalpostId,
                kanal,
                mottattDato.atStartOfDay(),
                InnsendingType.DIALOGMELDING,
                saksnummer,
                null,
                false
            )
        }
        assertThat(stegresultat).isEqualTo(Fullført)
    }

    @Test
    fun `hvis meldekort ikke inneholder strukturert dokument blir det ikke overført til fagsystem`() {
        val kontekst: FlytKontekst = mockk(relaxed = true)
        val digitaliseringsvurdering = Digitaliseringsvurdering(
            InnsendingType.MELDEKORT, null, mottattDato, true
        )
        every { struktureringsvurderingRepository.hentHvisEksisterer(any()) } returns digitaliseringsvurdering
        every { overleveringVurderingRepository.hentHvisEksisterer(any()) } returns null
        every { overleveringVurderingRepository.lagre(any(), any()) } returns Unit

        val stegresultat = overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 1) {
            overleveringVurderingRepository.lagre(any(), OverleveringVurdering(false))
        }
        verify(exactly = 0) {
            behandlingsflytKlient.sendHendelse(any(), any(), any(), any(), any(), any(), any())
        }
        assertThat(stegresultat).isEqualTo(Fullført)
    }
}