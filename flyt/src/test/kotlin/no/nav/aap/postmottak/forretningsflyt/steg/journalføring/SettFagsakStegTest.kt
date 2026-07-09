package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.avklaringsbehov.løsning.ForenkletDokument
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.VurderOpprettelseAvSakRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.VurderOpprettelseAvSakVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.gateway.AvsenderMottakerDto
import no.nav.aap.postmottak.gateway.JournalføringService
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.VurderOpprettelseAvSakValg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettFagsakStegTest {

    val saksnummerRepository: SaksnummerRepository = mockk(relaxed = true)
    val journalpostRepository: JournalpostRepository = mockk()
    val avklarTemaRepository: AvklarTemaRepository = mockk()
    val joark: JournalføringService = mockk(relaxed = true)
    val vurderOpprettelseAvSakRepository: VurderOpprettelseAvSakRepository = mockk(relaxed = true)

    val settFagsakSteg = SettFagsakSteg(journalpostRepository, saksnummerRepository, avklarTemaRepository, joark, vurderOpprettelseAvSakRepository)

    @Test
    fun `verifiser at journalpost blir oppdatert med saksnummer`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(true, Tema.AAP)
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

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
                dokumenter = vurdering.dokumenter
            )
        }
    }

    @Test
    fun `Skal sette avsenderMottaker til null hvis journalpost er digitalt innsendt`() {
        val journalpost: Journalpost = mockk(relaxed = true) {
            every { kanal } returns KanalFraKodeverk.NAV_NO
        }
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(true, Tema.AAP)
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost

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
                dokumenter = vurdering.dokumenter
            )
        }
    }

    @Test
    fun `går videre dersom journalpost ikke har tema AAP`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.erDigitalSøknad() } returns false
        every { avklarTemaRepository.hentTemaAvklaring(any()) } returns TemaVurdering(false, Tema.UKJENT)

        every { journalpostRepository.hentHvisEksisterer(any() as BehandlingId) } returns journalpost

        every { saksnummerRepository.hentSakVurdering(any() as BehandlingId) } throws IllegalStateException("Skal ikke treffe denne mocken")

        val resultat = settFagsakSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
    }

    @Test
    fun `journalpost som skal til Arena føres ikke på fagsak i Kelvin`() {
        val journalpost: Journalpost = mockk(relaxed = true)
        every { journalpost.erUgyldig() } returns false
        every { journalpostRepository.hentHvisEksisterer(any<BehandlingId>()) } returns journalpost
        every { vurderOpprettelseAvSakRepository.hentHvisEksisterer(any()) } returns
            VurderOpprettelseAvSakVurdering(valg = VurderOpprettelseAvSakValg.ARENA)

        val resultat = settFagsakSteg.utfør(mockk(relaxed = true))

        assertEquals(Fullført::class.simpleName, resultat::class.simpleName)
        verify(exactly = 0) { avklarTemaRepository.hentTemaAvklaring(any()) }
    }
}