package no.nav.aap.postmottak.forretningsflyt.steg

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.klient.saf.SafRestClient
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.klient.joark.Dokument
import no.nav.aap.postmottak.klient.joark.DokumentInfoId
import no.nav.aap.postmottak.klient.joark.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class OverleverTilFagsystemStegTest {

    val struktureringsvurderingRepository: StruktureringsvurderingRepository = mockk(relaxed = true)
    val kategorivurderingRepository: KategorivurderingRepository = mockk(relaxed = true)
    val behandlingsflytGateway: BehandlingsflytGateway = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepository = mockk()
    val saksnummerRepository: SaksnummerRepository = mockk()
    val safRestClient: SafRestClient = mockk(relaxed = true)

    val overførTilFagsystemSteg = OverleverTilFagsystemSteg(
        struktureringsvurderingRepository,
        kategorivurderingRepository,
        behandlingsflytGateway,
        journalpostRepository,
        saksnummerRepository,
        safRestClient
    )


    val kontekst: FlytKontekstMedPerioder = mockk(relaxed = true)
    val journalpost: Journalpost = mockk()
    val journalpostId: JournalpostId = JournalpostId(123)
    val behandling: Behandling = mockk()
    val saksnummer = "String"

    @BeforeEach
    fun beforeEach() {
        every { journalpostRepository.hentHvisEksisterer(any()) } returns journalpost
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

        every { journalpost.erSøknad() } returns false
        every { struktureringsvurderingRepository.hentStruktureringsavklaring(any())?.vurdering } returns """{
            |"yrkesskade": "Nei"},
            |"student": {"erStudent":"Nei", "kommeTilbake": "Nei"},
            |"oppgitteBarn": []
            |}""".trimMargin()
        every { kategorivurderingRepository.hentKategoriAvklaring(any())?.avklaring } returns Brevkode.SØKNAD

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 0) { safRestClient.hentDokument(any(), any()) }
        verify(exactly = 1) { behandlingsflytGateway.sendSøknad(saksnummer, journalpostId, any()) }
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
        every { journalpost.erSøknad()} returns true
        every {
            safRestClient.hentDokument(
                journalpostId,
                dokumentInfoId
            ).dokument
        } returns ByteArrayInputStream(journalpostJson.toByteArray())

        overførTilFagsystemSteg.utfør(kontekst)

        verify(exactly = 1) { safRestClient.hentDokument(journalpostId, dokumentInfoId) }
        verify(exactly = 1) { behandlingsflytGateway.sendSøknad(saksnummer, journalpostId, any()) }
    }
}