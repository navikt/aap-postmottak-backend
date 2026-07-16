package no.nav.aap.postmottak.avklaringsbehov.løser

import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.komponenter.httpklient.httpclient.error.BadRequestHttpResponsException
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarFordelingLøsning
import no.nav.aap.postmottak.avklaringsbehov.løsning.FordelingSystemValg
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

internal class AvklarFordelingLøserTest {

    private val avklarFordelingRepository = mockk<AvklarFordelingRepository>()
    private val løser = AvklarFordelingLøser(avklarFordelingRepository)

    @Test
    fun `lagrer ARENA-vurdering med kommentar`() {
        val kontekst = opprettKontekst()
        justRun { avklarFordelingRepository.lagreVurdering(any(), any()) }

        val resultat = løser.løs(
            kontekst,
            AvklarFordelingLøsning(FordelingSystemValg.ARENA, kommentar = "Til Arena")
        )

        assertThat(resultat.begrunnelse).contains("ARENA")
        verify(exactly = 1) {
            avklarFordelingRepository.lagreVurdering(eq(kontekst.kontekst.behandlingId), withArg {
                assertThat(it.system).isEqualTo(AapSystem.ARENA)
                assertThat(it.kommentar).isEqualTo("Til Arena")
                assertThat(it.vurdertAv).isEqualTo("ident")
            })
        }
    }

    @Test
    fun `lagrer KELVIN-vurdering`() {
        val kontekst = opprettKontekst()
        justRun { avklarFordelingRepository.lagreVurdering(any(), any()) }

        løser.løs(kontekst, AvklarFordelingLøsning(FordelingSystemValg.KELVIN))

        verify(exactly = 1) {
            avklarFordelingRepository.lagreVurdering(eq(kontekst.kontekst.behandlingId), withArg {
                assertThat(it.system).isEqualTo(AapSystem.KELVIN)
            })
        }
    }

    @Test
    fun `BEGGE blokkeres på løsing og lagrer ingenting`() {
        val kontekst = opprettKontekst()

        assertThrows<BadRequestHttpResponsException> {
            løser.løs(kontekst, AvklarFordelingLøsning(FordelingSystemValg.BEGGE))
        }

        verify(exactly = 0) { avklarFordelingRepository.lagreVurdering(any(), any()) }
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

