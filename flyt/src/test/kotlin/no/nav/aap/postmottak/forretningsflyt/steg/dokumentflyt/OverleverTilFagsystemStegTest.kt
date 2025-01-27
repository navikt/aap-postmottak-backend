package no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.FunnetAvklaringsbehov
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class OverleverTilFagsystemStegTest {

    val struktureringsvurderingRepository: StruktureringsvurderingRepository = mockk(relaxed = true)
    val behandlingsflytKlient: BehandlingsflytGateway = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepository = mockk()
    val saksnummerRepository: SaksnummerRepository = mockk()
    val overleveringVurderingRepository: OverleveringVurderingRepository = mockk()

    val overførTilFagsystemSteg = OverleverTilFagsystemSteg(
        struktureringsvurderingRepository,
        behandlingsflytKlient,
        journalpostRepository,
        saksnummerRepository,
        overleveringVurderingRepository
    )


    val kontekst: FlytKontekstMedPerioder = mockk(relaxed = true)
    val journalpost: Journalpost = mockk()
    val journalpostId: JournalpostId = JournalpostId(123)
    val behandling: Behandling = mockk()
    val saksnummer = "String"

    @BeforeEach
    fun beforeEach() {
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { journalpost.journalpostId } returns journalpostId
        every { behandling.journalpostId } returns journalpostId
        every { saksnummerRepository.hentSakVurdering(any())?.saksnummer } returns saksnummer
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }


    @Test
    fun `hvis søknad er manuelt strukturert, blir strukturert dokument sendt til behandlingsflyt`() {
        val kontekst: FlytKontekstMedPerioder = mockk(relaxed = true)
        val struktureringsvurdering = Digitaliseringsvurdering(InnsendingType.SØKNAD, """{
            |"yrkesskade": "Nei",
            |"student": {"erStudent":"Nei", "kommeTilbake": "Nei"}
            |}""".trimMargin())
        every { journalpost.erDigitalSøknad() } returns false
        every { overleveringVurderingRepository.hentHvisEksisterer(any()) } returns null
        every { overleveringVurderingRepository.lagre(any(), any()) } returns Unit
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns struktureringsvurdering

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 1) {
            behandlingsflytKlient.sendHendelse(
                journalpost,
                InnsendingType.SØKNAD,
                saksnummer,
                DokumentTilMeldingParser
                    .parseTilMelding(struktureringsvurdering.strukturertDokument, InnsendingType.SØKNAD)
            )
        }
    }

    @Test
    fun `hvis automatisk journalføring blir digital søknad fra joark sendt til behandlingsflyt`() {
        val dokument: Dokument = mockk()
        val dokumentInfoId: DokumentInfoId = mockk()

        val journalpostJson = """{
            |"yrkesskade": "Nei",
            |"student": {"erStudent": "Nei", "kommeTilbake": "Nei"},
            |"oppgitteBarn": {"identer": []}
            |}""".trimMargin()

        every { dokument.dokumentInfoId } returns dokumentInfoId
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns Digitaliseringsvurdering(
            InnsendingType.SØKNAD, journalpostJson
        )
        every { overleveringVurderingRepository.hentHvisEksisterer(any()) } returns null
        every { overleveringVurderingRepository.lagre(any(), any()) } returns Unit

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 1) {
            behandlingsflytKlient.sendHendelse(
                journalpost,
                InnsendingType.SØKNAD,
                saksnummer,
                DokumentTilMeldingParser
                    .parseTilMelding(journalpostJson, InnsendingType.SØKNAD)
            )
        }
    }

    @Test
    fun `hvis journalposten er dialogmelding kreves manuell avklaring`() {
        val dokument: Dokument = mockk()
        val dokumentInfoId: DokumentInfoId = mockk()
        every { dokument.dokumentInfoId } returns dokumentInfoId
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any())?.kategori } returns InnsendingType.DIALOGMELDING
        every { overleveringVurderingRepository.hentHvisEksisterer(any()) } returns null

        val stegresultat = overførTilFagsystemSteg.utfør(kontekst)
        assertEquals(stegresultat::class.simpleName, FantAvklaringsbehov::class.simpleName)
        val funnetAvklaringsbehov = stegresultat.transisjon() as FunnetAvklaringsbehov
        assertThat(funnetAvklaringsbehov.avklaringsbehov()).contains(Definisjon.AVKLAR_OVERLEVERING)
    }

    @Test
    fun `dialogmelding som skal til behandlingsflyt blir sendt korrekt`() {
        val dokument: Dokument = mockk()
        val dokumentInfoId: DokumentInfoId = mockk()
        every { dokument.dokumentInfoId } returns dokumentInfoId
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns Digitaliseringsvurdering(InnsendingType.DIALOGMELDING, null)
        every { overleveringVurderingRepository.hentHvisEksisterer(any()) } returns OverleveringVurdering(true)

        val stegresultat = overførTilFagsystemSteg.utfør(kontekst)
        verify(exactly = 1) {
            behandlingsflytKlient.sendHendelse(
                journalpost,
                InnsendingType.DIALOGMELDING,
                saksnummer,
                null
            )
        }
        assertThat(stegresultat::class.simpleName).isEqualTo(Fullført::class.simpleName)
    }
}