package no.nav.aap.postmottak.forretningsflyt.steg.fordeling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.FordelerRegelService
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.fordeler.arena.AvklarFordelingVurdering
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.gateway.Bruker
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.TestObjekter.lagTestJournalpost
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class AvklarFordelingStegTest {

    private val journalpostId = JournalpostId(1L)
    private val behandlingId = BehandlingId(1L)
    private val kontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Fordeling)

    private val regelService = mockk<FordelerRegelService>(relaxed = true)
    private val journalpostService = mockk<JournalpostService>(relaxed = true)
    private val avklarFordelingRepository = mockk<AvklarFordelingRepository>(relaxed = true)
    private val innkommendeJournalpostRepository = mockk<InnkommendeJournalpostRepository>(relaxed = true).also {
        // Standard: journalposten er lagret av VurderRelevantDokumentForAAPJobbUtfører, men ikke evaluert
        every { it.hentHvisEksisterer(journalpostId) } returns InnkommendeJournalpost(
            journalpostId = journalpostId,
            brevkode = Brevkoder.SØKNAD.kode,
            behandlingstema = null,
            status = InnkommendeJournalpostStatus.EVALUERT,
            regelresultat = null,
        )
    }

    private val steg = AvklarFordelingSteg(
        regelService,
        journalpostService,
        avklarFordelingRepository,
        innkommendeJournalpostRepository,
    )

    @Test
    fun `Returnerer Fullført uten å evaluere om vurdering allerede eksisterer`() {
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns
            AvklarFordelingVurdering(AapSystem.KELVIN, "KELVIN", LocalDateTime.now())

        steg.utfør(kontekst)

        verify(exactly = 0) { regelService.evaluer(any()) }
        verify(exactly = 0) { avklarFordelingRepository.lagreVurdering(any(), any()) }
    }

    @Test
    fun `Oppdaterer innkommendeJournalpost med regelresultat og lagrer vurdering etter vellykket evaluering`() {
        val regelResultat = Regelresultat(
            mapOf(
                "ArenaSakRegel" to false,
                "KelvinSakRegel" to false,
                "ErIkkeReisestønadRegel" to true,
                "ErIkkeAnkeRegel" to true,
            ),
            forJournalpost = journalpostId.referanse,
        )

        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns null
        every { journalpostService.hentSafJournalpost(journalpostId) } returns lagTestJournalpost(journalpostId)
        every { regelService.evaluer(any()) } returns regelResultat

        steg.utfør(kontekst)

        verify {
            innkommendeJournalpostRepository.update(withArg {
                assertThat(it.journalpostId).isEqualTo(journalpostId)
                assertThat(it.regelresultat).isEqualTo(regelResultat)
                assertThat(it.status).isEqualTo(InnkommendeJournalpostStatus.EVALUERT)
            })
        }
        verify { avklarFordelingRepository.lagreVurdering(eq(behandlingId), any()) }
    }

    @Test
    fun `Evaluerer ikke på nytt om journalposten allerede har regelresultat, og bruker lagret resultat`() {
        val lagretResultat = regelresultat(kelvin = true)
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns null
        every { innkommendeJournalpostRepository.hentHvisEksisterer(journalpostId) } returns
            innkommendeJournalpost(regelresultat = lagretResultat)
        every { journalpostService.hentSafJournalpost(journalpostId) } returns lagTestJournalpost(journalpostId)

        steg.utfør(kontekst)

        verify(exactly = 0) { regelService.evaluer(any()) }
        verify(exactly = 0) { innkommendeJournalpostRepository.update(any()) }
        verify(exactly = 0) { innkommendeJournalpostRepository.lagre(any()) }
        verify {
            avklarFordelingRepository.lagreVurdering(eq(behandlingId), withArg {
                assertThat(it.system).isEqualTo(AapSystem.KELVIN)
            })
        }
    }

    @Test
    fun `Vurderes som IGNORERT dersom journalpost med orgnr ikke har innkommende journalpost`() {
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns null
        every { innkommendeJournalpostRepository.hentHvisEksisterer(journalpostId) } returns null
        every { journalpostService.hentSafJournalpost(journalpostId) } returns lagTestJournalpost(journalpostId)
            .copy(bruker = Bruker(id = "999999999", type = BrukerIdType.ORGNR))

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(Fullført)
        verify(exactly = 0) { regelService.evaluer(any()) }
        verify {
            avklarFordelingRepository.lagreVurdering(eq(behandlingId), withArg {
                assertThat(it.system).isEqualTo(AapSystem.IGNORERT)
                assertThat(it.vurdertAv).isEqualTo(SYSTEMBRUKER.ident)
            })
        }
    }

    @Test
    fun `Feiler dersom journalpost med fnr ikke har innkommende journalpost`() {
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns null
        every { innkommendeJournalpostRepository.hentHvisEksisterer(journalpostId) } returns null
        every { journalpostService.hentSafJournalpost(journalpostId) } returns lagTestJournalpost(journalpostId)

        assertThatThrownBy { steg.utfør(kontekst) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Journalposten skal allerede være lagret")

        verify(exactly = 0) { regelService.evaluer(any()) }
        verify(exactly = 0) { avklarFordelingRepository.lagreVurdering(any(), any()) }
    }

    @Test
    fun `Automatisk vurdering til Kelvin når regelresultat sier Kelvin`() {
        settOppAutomatiskVurdering(regelresultat(kelvin = true))

        steg.utfør(kontekst)

        verify {
            avklarFordelingRepository.lagreVurdering(eq(behandlingId), withArg {
                assertThat(it.system).isEqualTo(AapSystem.KELVIN)
                assertThat(it.vurdertAv).isEqualTo(SYSTEMBRUKER.ident)
                assertThat(it.kommentar).isEqualTo("Automatisk vurdert fordeling")
            })
        }
    }

    @Test
    fun `Automatisk vurdering til Arena når regelresultat ikke sier Kelvin`() {
        settOppAutomatiskVurdering(regelresultat(kelvin = false))

        steg.utfør(kontekst)

        verify {
            avklarFordelingRepository.lagreVurdering(eq(behandlingId), withArg {
                assertThat(it.system).isEqualTo(AapSystem.ARENA)
                assertThat(it.vurdertAv).isEqualTo(SYSTEMBRUKER.ident)
            })
        }
    }

    @Test
    fun `Oppdaterer eksisterende rad og bevarer øvrige felter ved evaluering`() {
        val eksisterende = innkommendeJournalpost(regelresultat = null).copy(enhet = "4491", brukerId = "fnr")
        val res = regelresultat(kelvin = false)
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns null
        every { innkommendeJournalpostRepository.hentHvisEksisterer(journalpostId) } returns eksisterende
        every { journalpostService.hentSafJournalpost(journalpostId) } returns lagTestJournalpost(journalpostId)
        every { regelService.evaluer(any()) } returns res

        steg.utfør(kontekst)

        verify(exactly = 1) { innkommendeJournalpostRepository.update(eksisterende.copy(regelresultat = res)) }
        verify(exactly = 0) { innkommendeJournalpostRepository.lagre(any()) }
    }

    private fun settOppAutomatiskVurdering(res: Regelresultat) {
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns null
        every { journalpostService.hentSafJournalpost(journalpostId) } returns lagTestJournalpost(journalpostId)
        every { regelService.evaluer(any()) } returns res
    }

    private fun regelresultat(kelvin: Boolean) = Regelresultat(
        mapOf(
            "ArenaSakRegel" to !kelvin,
            "KelvinSakRegel" to kelvin,
            "ErIkkeReisestønadRegel" to true,
            "ErIkkeAnkeRegel" to true,
            // Arena krever at minst én av de øvrige reglene gir false
            "ManueltOverstyrtTilArenaRegel" to !kelvin,
        ),
        forJournalpost = journalpostId.referanse,
    )

    private fun innkommendeJournalpost(regelresultat: Regelresultat?) = InnkommendeJournalpost(
        journalpostId = journalpostId,
        brevkode = Brevkoder.SØKNAD.kode,
        behandlingstema = null,
        status = InnkommendeJournalpostStatus.EVALUERT,
        regelresultat = regelresultat,
    )
}

