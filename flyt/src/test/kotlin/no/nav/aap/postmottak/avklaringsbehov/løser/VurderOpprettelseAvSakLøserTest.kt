package no.nav.aap.postmottak.avklaringsbehov.løser

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlin.random.Random
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.arena.ArenaVideresender
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.VurderOpprettelseAvSakLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.VurderOpprettelseAvSakRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.VurderOpprettelseAvSakVurdering
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.VurderOpprettelseAvSakValg
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VurderOpprettelseAvSakLøserTest {

    private val saksnummerRepository = mockk<SaksnummerRepository>(relaxed = true)
    private val journalpostRepository = mockk<JournalpostRepository>()
    private val behandlingsflytGateway = mockk<BehandlingsflytGateway>()
    private val vurderOpprettelseAvSakRepository = mockk<VurderOpprettelseAvSakRepository>(relaxed = true)
    private val innkommendeJournalpostRepository = mockk<InnkommendeJournalpostRepository>()
    private val arenaVideresender = mockk<ArenaVideresender>(relaxed = true)

    private val løser = VurderOpprettelseAvSakLøser(
        saksnummerRepository,
        journalpostRepository,
        behandlingsflytGateway,
        vurderOpprettelseAvSakRepository,
        innkommendeJournalpostRepository,
        { arenaVideresender },
    )

    @Test
    fun `KELVIN oppretter ny sak i Kelvin og lagrer valget`() {
        val kontekst = opprettKontekst()
        val løsning = VurderOpprettelseAvSakLøsning(valg = VurderOpprettelseAvSakValg.KELVIN, begrunnelse = "ny sak")
        val ident = "01020312345"

        every { journalpostRepository.hentHvisEksisterer(kontekst.kontekst.behandlingId) } returns mockk {
            every { person.aktivIdent().identifikator } returns ident
            every { mottattDato } returns mockk()
        }
        every { behandlingsflytGateway.finnEllerOpprettSak(any(), any()) } returns mockk {
            every { saksnummer } returns "99999"
        }

        val result = løser.løs(kontekst, løsning)

        assertTrue(result.begrunnelse.contains("Kelvin"))
        verify(exactly = 1) {
            vurderOpprettelseAvSakRepository.lagre(
                kontekst.kontekst.behandlingId,
                VurderOpprettelseAvSakVurdering(valg = VurderOpprettelseAvSakValg.KELVIN, begrunnelse = "ny sak")
            )
            behandlingsflytGateway.finnEllerOpprettSak(Ident(ident), any())
            saksnummerRepository.lagreSakVurdering(
                kontekst.kontekst.behandlingId,
                Saksvurdering(saksnummer = "99999", generellSak = false, opprettetNy = true)
            )
        }
    }

    @Test
    fun `ARENA videresender journalposten til Arena og lagrer valget`() {
        val kontekst = opprettKontekst()
        val løsning = VurderOpprettelseAvSakLøsning(valg = VurderOpprettelseAvSakValg.ARENA)

        every { innkommendeJournalpostRepository.hentId(kontekst.kontekst.journalpostId) } returns 42L

        val result = løser.løs(kontekst, løsning)

        assertTrue(result.begrunnelse.contains("Arena"))
        verify(exactly = 1) {
            vurderOpprettelseAvSakRepository.lagre(
                kontekst.kontekst.behandlingId,
                VurderOpprettelseAvSakVurdering(valg = VurderOpprettelseAvSakValg.ARENA)
            )
            arenaVideresender.videresendJournalpostTilArena(kontekst.kontekst.journalpostId, 42L)
        }
        verify(exactly = 0) { behandlingsflytGateway.finnEllerOpprettSak(any(), any()) }
    }

    @Test
    fun `BEGGE er ikke støttet enda`() {
        val kontekst = opprettKontekst()
        val løsning = VurderOpprettelseAvSakLøsning(valg = VurderOpprettelseAvSakValg.BEGGE)

        assertThrows<NotImplementedError> {
            løser.løs(kontekst, løsning)
        }
    }

    @Test
    fun `Mangler valg gir feil`() {
        val kontekst = opprettKontekst()
        val løsning = VurderOpprettelseAvSakLøsning(valg = null)

        assertThrows<IllegalStateException> {
            løser.løs(kontekst, løsning)
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


