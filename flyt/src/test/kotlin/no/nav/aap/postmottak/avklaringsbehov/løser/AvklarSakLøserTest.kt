package no.nav.aap.postmottak.avklaringsbehov.løser

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlin.random.Random
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.AvslagException
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarSaksnummerLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AvklarSakLøserTest {

    private val saksnummerRepository = mockk<SaksnummerRepository>()
    private val journalpostRepository = mockk<JournalpostRepository>()
    private val behandlingsflytGateway = mockk<BehandlingsflytGateway>()

    private val avklarSakLøser =
        AvklarSakLøser(saksnummerRepository, journalpostRepository, behandlingsflytGateway)


    @Test
    fun `Skal kaste feil AvslagException hvis opprettNySak=true og det eksisterer avslag på tidligere behandling`() {
        val kontekst = opprettKontekst()
        val løsning = AvklarSaksnummerLøsning(null, opprettNySak = true)

        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(any()) } returns true

        assertThrows<AvslagException> {
            avklarSakLøser.løs(kontekst, løsning)
        }
    }

    @Test
    fun `Opprett ny sak fungerer som forventet`() {
        val kontekst = opprettKontekst()
        val løsning = AvklarSaksnummerLøsning(null, opprettNySak = true)
        val ident = "01020312345"

        every { saksnummerRepository.eksistererAvslagPåTidligereBehandling(kontekst.kontekst.behandlingId) } returns false
        every { journalpostRepository.hentHvisEksisterer(kontekst.kontekst.behandlingId) } returns mockk {
            every { hoveddokumentbrevkode } returns "Ukjent"
            every { erSøknad() } returns true
            every { person.aktivIdent().identifikator } returns ident
            every { mottattDato } returns mockk()
        }
        every { behandlingsflytGateway.finnEllerOpprettSak(any(), any()) } returns mockk {
            every { saksnummer } returns "99999"
        }
        justRun { saksnummerRepository.lagreSakVurdering(any(), any()) }

        val result = avklarSakLøser.løs(kontekst, løsning)

        assertTrue(result.begrunnelse.contains("Dokument er tildelt sak"))

        verify(exactly = 1) {
            behandlingsflytGateway.finnEllerOpprettSak(Ident(ident), any())

            saksnummerRepository.lagreSakVurdering(
                behandlingId = kontekst.kontekst.behandlingId,
                saksvurdering = Saksvurdering(
                    saksnummer = "99999",
                    generellSak = false,
                    opprettetNy = true,
                    journalposttittel = løsning.journalposttittel,
                    avsenderMottaker = løsning.avsenderMottaker,
                    dokumenter = løsning.dokumenter,
                )
            )
        }
    }

    @Test
    fun `Skal lagre vurdering på eksisterende sak`() {
        val kontekst = opprettKontekst()
        val løsning = AvklarSaksnummerLøsning("12345", opprettNySak = false)

        justRun { saksnummerRepository.lagreSakVurdering(kontekst.kontekst.behandlingId, any()) }

        val result = avklarSakLøser.løs(kontekst, løsning)

        assertTrue(result.begrunnelse.contains("Dokument er tildelt sak"))
        verify(exactly = 1) {
            saksnummerRepository.lagreSakVurdering(
                behandlingId = kontekst.kontekst.behandlingId,
                saksvurdering = Saksvurdering(
                    saksnummer = løsning.saksnummer,
                    generellSak = false,
                    opprettetNy = false,
                    journalposttittel = løsning.journalposttittel,
                    avsenderMottaker = løsning.avsenderMottaker,
                    dokumenter = løsning.dokumenter,
                )
            )
        }
    }

    private fun opprettKontekst() =
        AvklaringsbehovKontekst(
            bruker = Bruker("ident"),
            kontekst = FlytKontekst(
                journalpostId = JournalpostId(Random.nextLong()),
                behandlingId = BehandlingId(Random.nextLong()),
                behandlingType = TypeBehandling.Journalføring
            )
        )

}
