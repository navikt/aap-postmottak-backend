package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.avklaringsbehov.løsning.ForenkletDokument
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.gateway.AvsenderMottaker
import no.nav.aap.postmottak.gateway.AvsenderMottakerDto
import no.nav.aap.postmottak.gateway.AvsenderMottakerIdType
import no.nav.aap.postmottak.gateway.JournalføringService
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import no.nav.aap.komponenter.verdityper.Bruker

class SettFagsakStegTest {

    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepository = mockk()
    val avklarTemaRepository: AvklarTemaRepository = mockk()
    val joark: JournalføringService = mockk(relaxed = true)
    val avklaringsbehovRepository: AvklaringsbehovRepository = mockk(relaxed = true)

    val settFagsakSteg = SettFagsakSteg(
        journalpostRepository,
        saksnummerRepository,
        avklarTemaRepository,
        joark,
        avklaringsbehovRepository
    )

    @ParameterizedTest
    @EnumSource(Tema::class, names = ["AAP", "OPP", "UKJENT"], mode = EnumSource.Mode.EXCLUDE)
    fun `andre kjente temaer endrer kun tema i Joark`(tema: Tema) {
        val journalpost = TestJournalposter.leggTil().tilJournalpost()
        val bruker = Bruker("SAKSBEHANDLER")
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, tema)
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every {
            avklaringsbehovRepository.hentAvklaringsbehovene(any()).hvemSomLøste(Definisjon.AVKLAR_TEMA)
        } returns bruker

        assertEquals(Fullført, settFagsakSteg.utfør(mockk(relaxed = true)))
        verify(exactly = 1) { joark.endreTema(journalpost.journalpostId, tema.name, bruker) }
        verify(exactly = 0) { saksnummerRepository.hentSakVurdering(any()) }
    }

    @Test
    fun `overskriver ikke tema endret utenfra`() {
        val journalpost = TestJournalposter.leggTil { tema = "DAG" }.tilJournalpost()
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, Tema.BAR)
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

        assertEquals(Fullført, settFagsakSteg.utfør(mockk(relaxed = true)))
        verify(exactly = 0) { joark.endreTema(any(), any(), any()) }
    }

    @Test
    fun `feil fra Joark stopper flyten`() {
        val journalpost = TestJournalposter.leggTil().tilJournalpost()
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, Tema.BAR)
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { joark.endreTema(any(), any(), any()) } throws IllegalStateException("Joark utilgjengelig")

        assertThrows<IllegalStateException> { settFagsakSteg.utfør(mockk(relaxed = true)) }
    }

    @Test
    fun `verifiser at journalpost blir oppdatert med saksnummer`() {
        val journalpost = TestJournalposter.leggTil {
            kanal = KanalFraKodeverk.SKAN_NETS
            avsenderMottaker = AvsenderMottaker("id", AvsenderMottakerIdType.FNR, "navn")
        }
            .tilJournalpost()
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(true, Tema.AAP)
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every {
            avklaringsbehovRepository.hentAvklaringsbehovene(any()).hvemSomLøste(Definisjon.AVKLAR_SAK)
        } returns null

        val vurdering = Saksvurdering(
            "12345",
            journalposttittel = "Tittel",
            avsenderMottaker = AvsenderMottakerDto("id", AvsenderMottakerDto.IdType.FNR, "navn"),
            dokumenter = listOf(ForenkletDokument("123", "hoveddokument tittel"))
        )

        every { saksnummerRepository.hentSakVurdering(any()) } returns vurdering

        settFagsakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) {
            joark.førJournalpostPåFagsak(
                journalpost.journalpostId,
                journalpost.person.aktivIdent(),
                vurdering.saksnummer!!,
                tittel = vurdering.journalposttittel,
                avsenderMottaker = vurdering.avsenderMottaker,
                dokumenter = vurdering.dokumenter,
                endretAv = null,
            )
        }
    }

    @Test
    fun `Skal sette avsenderMottaker til null hvis journalpost er digitalt innsendt`() {
        val journalpost = TestJournalposter.leggTil {
            kanal = KanalFraKodeverk.NAV_NO
        }
            .tilJournalpost()

        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(true, Tema.AAP)
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every {
            avklaringsbehovRepository.hentAvklaringsbehovene(any()).hvemSomLøste(Definisjon.AVKLAR_SAK)
        } returns null

        val vurdering = Saksvurdering(
            "12345",
            journalposttittel = "Tittel",
            avsenderMottaker = AvsenderMottakerDto("id", AvsenderMottakerDto.IdType.FNR, "navn"),
            dokumenter = listOf(ForenkletDokument("123", "hoveddokument tittel"))
        )

        every { saksnummerRepository.hentSakVurdering(any()) } returns vurdering

        settFagsakSteg.utfør(mockk(relaxed = true))

        verify(exactly = 1) {
            joark.førJournalpostPåFagsak(
                journalpost.journalpostId,
                journalpost.person.aktivIdent(),
                vurdering.saksnummer!!,
                tittel = vurdering.journalposttittel,
                avsenderMottaker = null,
                dokumenter = vurdering.dokumenter,
                endretAv = null,
            )
        }
    }

    @Test
    fun `går videre dersom journalpost ikke har tema AAP`() {
        val journalpost = TestJournalposter.leggTil {
            tema = "NOEANNET"
            kanal = KanalFraKodeverk.SKAN_NETS
        }
            .tilJournalpost()
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, Tema.UKJENT)

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        every { saksnummerRepository.hentSakVurdering(any() as BehandlingId) } throws IllegalStateException("Skal ikke treffe denne mocken")

        val resultat = settFagsakSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }
}