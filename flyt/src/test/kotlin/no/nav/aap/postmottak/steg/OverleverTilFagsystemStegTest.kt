package no.nav.aap.postmottak.steg

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.klient.saf.SafRestKlient
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.forretningsflyt.steg.OverleverTilFagsystemSteg
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.DokumentGateway
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class OverleverTilFagsystemStegTest {

    val struktureringsvurderingRepository: StruktureringsvurderingRepository = mockk(relaxed = true)
    val kategorivurderingRepository: KategoriVurderingRepository = mockk(relaxed = true)
    val behandlingsflytKlient: BehandlingsflytGateway = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepository = mockk()
    val saksnummerRepository: SaksnummerRepository = mockk()
    val safRestKlient: DokumentGateway = mockk(relaxed = true)

    val overførTilFagsystemSteg = OverleverTilFagsystemSteg(
        struktureringsvurderingRepository,
        kategorivurderingRepository,
        behandlingsflytKlient,
        journalpostRepository,
        saksnummerRepository,
        safRestKlient
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
    fun `hvis journalpost er manuelt strukturert, blir strukturert dokument sendt til behandlingsflyt`() {

        val kontekst: FlytKontekstMedPerioder = mockk(relaxed = true)

        every { journalpost.erDigitalSøknad() } returns false
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any())?.vurdering } returns """{
            |"yrkesskade": "Nei",
            |"student": {"erStudent":"Nei", "kommeTilbake": "Nei"}
            |}""".trimMargin()
        every { kategorivurderingRepository.hentKategoriAvklaring(any())?.avklaring } returns InnsendingType.SØKNAD

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 0) { safRestKlient.hentDokument(any(), any()) }
        verify(exactly = 1) { behandlingsflytKlient.sendHendelse(journalpost, saksnummer, any()) }
    }

    @Test
    fun `hvis automatisk journalføring blir strukturert dokument fra joark sendt til behandlingsflyt`() {
        val dokument: Dokument = mockk()
        val dokumentInfoId: DokumentInfoId = mockk()
        
        val journalpostJson = """{
            |"yrkesskade": "Nei",
            |"student": {"erStudent": "Nei", "kommeTilbake": "Nei"},
            |"oppgitteBarn": {"identer": []}
            |}""".trimMargin()

        every { dokument.dokumentInfoId } returns dokumentInfoId
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any()) } returns null
        every { journalpost.finnOriginal() } returns dokument
        every {journalpost.erDigitalSøknad()} returns true
        every {
            safRestKlient.hentDokument(
                journalpostId,
                dokumentInfoId
            ).dokument
        } returns ByteArrayInputStream(journalpostJson.toByteArray())

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 1) { safRestKlient.hentDokument(journalpostId, dokumentInfoId) }
        verify(exactly = 1) { behandlingsflytKlient.sendHendelse(journalpost, saksnummer, any()) }
    }
}