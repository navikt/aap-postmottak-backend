package no.nav.aap.postmottak.forretningsflyt.steg.fordeling

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.fordeler.FordelerRegelService
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.fordeler.arena.ArenaService
import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.fordeler.arena.AvklarFordelingVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.gateway.Bruker
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafDokumentInfo
import no.nav.aap.postmottak.gateway.SafDokumentvariant
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafVariantformat
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.TestObjekter.lagTestJournalpost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class AvklarFordelingStegTest {

    private val regelService = mockk<FordelerRegelService>(relaxed = true)
    private val journalpostService = mockk<JournalpostService>(relaxed = true)
    private val enhetsutreder = mockk<Enhetsutreder>()
    private val avklarFordelingRepository = mockk<AvklarFordelingRepository>(relaxed = true)
    private val innkommendeJournalpostRepository = mockk<InnkommendeJournalpostRepository>(relaxed = true)
    private val gosysOppgaveGateway = mockk<GosysOppgaveGateway>(relaxed = true)
    private val arenaService = mockk<ArenaService>(relaxed = true)
    private val arenaoppslagGateway = mockk<ArenaoppslagGateway>(relaxed = true)

    private val steg = AvklarFordelingSteg(
        regelService,
        journalpostService,
        enhetsutreder,
        avklarFordelingRepository,
        innkommendeJournalpostRepository,
        gosysOppgaveGateway,
        arenaService,
        arenaoppslagGateway,
    )

    private val journalpostId = JournalpostId(1L)
    private val behandlingId = BehandlingId(1L)
    private val kontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Fordeling)

    @Test
    fun `Returnerer Fullført uten å evaluere om vurdering allerede eksisterer`() {
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns
            AvklarFordelingVurdering(AapSystem.KELVIN, "KELVIN", LocalDateTime.now())

        steg.utfør(kontekst)

        verify(exactly = 0) { regelService.evaluer(any()) }
        verify(exactly = 0) { avklarFordelingRepository.lagreVurdering(any(), any()) }
    }

    @Test
    fun `Lagrer innkommendeJournalpost og vurdering etter vellykket evaluering`() {
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
        every { enhetsutreder.finnJournalføringsenhet(any()) } returns "1234"
        every { journalpostService.hentSafJournalpost(journalpostId) } returns lagTestJournalpost(journalpostId)
        every { regelService.evaluer(any()) } returns regelResultat

        steg.utfør(kontekst)

        verify {
            innkommendeJournalpostRepository.lagre(withArg {
                assertThat(it.journalpostId).isEqualTo(journalpostId)
                assertThat(it.regelresultat).isEqualTo(regelResultat)
                assertThat(it.status).isEqualTo(InnkommendeJournalpostStatus.EVALUERT)
            })
        }
        verify { avklarFordelingRepository.lagreVurdering(eq(behandlingId), any()) }
    }

    @Test
    fun `Returnerer FantAvklaringsbehov og lagrer ikke vurdering når søknaden skal til manuell vurdering`() {
        val regelResultat = Regelresultat(
            mapOf(
                "ArenaSakRegel" to true,
                "KelvinSakRegel" to false,
                "ErIkkeReisestønadRegel" to true,
                "ErIkkeAnkeRegel" to true,
            ),
            forJournalpost = journalpostId.referanse,
        )

        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns null
        every { enhetsutreder.finnJournalføringsenhet(any()) } returns "1234"
        every { journalpostService.hentSafJournalpost(journalpostId) } returns lagTestJournalpost(journalpostId)
        every { regelService.evaluer(any()) } returns regelResultat
        coEvery { arenaService.skalManueltFordeles(any(), any(), any(), any()) } returns true

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isInstanceOf(FantAvklaringsbehov::class.java)
        verify { innkommendeJournalpostRepository.lagre(any()) }
        verify(exactly = 0) { avklarFordelingRepository.lagreVurdering(any(), any()) }
    }

    @Test
    fun `Evaluerer ikke og lagrer IGNORERT vurdering om journalposten allerede er evaluert`() {
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns null
        every { innkommendeJournalpostRepository.eksisterer(journalpostId) } returns true

        steg.utfør(kontekst)

        verify(exactly = 0) { regelService.evaluer(any()) }
        verify {
            avklarFordelingRepository.lagreVurdering(eq(behandlingId), withArg {
                assertThat(it.system).isEqualTo(AapSystem.IGNORERT)
            })
        }
    }

    @Test
    fun `Lagrer IGNORERT vurdering for utgaatt journalpost`() {
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns null
        every { journalpostService.hentSafJournalpost(journalpostId) } returns
            lagTestJournalpost(journalpostId).copy(journalstatus = Journalstatus.UTGAAR)

        steg.utfør(kontekst)

        verify(exactly = 0) { regelService.evaluer(any()) }
        verify {
            avklarFordelingRepository.lagreVurdering(eq(behandlingId), withArg {
                assertThat(it.system).isEqualTo(AapSystem.IGNORERT)
            })
        }
    }

    @Test
    fun `Oppretter Gosys fordelingsoppgave og lagrer IGNORERT vurdering for journalpost uten bruker-id`() {
        val journalpostUtenBruker = SafJournalpost(
            journalpostId = journalpostId.referanse,
            bruker = Bruker(id = null, type = BrukerIdType.FNR),
            dokumenter = listOf(
                SafDokumentInfo(
                    dokumentInfoId = "1",
                    brevkode = "NAV 11-13.05",
                    tittel = "tittel",
                    dokumentvarianter = listOf(
                        SafDokumentvariant(variantformat = SafVariantformat.ORIGINAL, filtype = "json")
                    )
                )
            ),
            journalstatus = Journalstatus.MOTTATT,
            tema = Tema.AAP.name,
            relevanteDatoer = emptyList()
        )
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns null
        every { journalpostService.hentSafJournalpost(journalpostId) } returns journalpostUtenBruker

        steg.utfør(kontekst)

        verify {
            gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(
                journalpostId = journalpostId,
                personIdent = null,
                orgnr = null,
                beskrivelse = "tittel"
            )
        }
        verify(exactly = 0) { innkommendeJournalpostRepository.lagre(any()) }
        verify {
            avklarFordelingRepository.lagreVurdering(eq(behandlingId), withArg {
                assertThat(it.system).isEqualTo(AapSystem.IGNORERT)
            })
        }
    }

    @Test
    fun `Oppretter Gosys fordelingsoppgave og lagrer IGNORERT vurdering for journalpost med orgnummer`() {
        val journalpostMedOrgnr = SafJournalpost(
            journalpostId = journalpostId.referanse,
            bruker = Bruker(id = "orgnr", type = BrukerIdType.ORGNR),
            dokumenter = listOf(
                SafDokumentInfo(
                    dokumentInfoId = "1",
                    brevkode = "NAV 11-13.05",
                    tittel = "tittel",
                    dokumentvarianter = listOf(
                        SafDokumentvariant(variantformat = SafVariantformat.ORIGINAL, filtype = "json")
                    )
                )
            ),
            journalstatus = Journalstatus.MOTTATT,
            tema = Tema.AAP.name,
            relevanteDatoer = emptyList()
        )
        every { avklarFordelingRepository.hentVurderingHvisEksisterer(behandlingId) } returns null
        every { journalpostService.hentSafJournalpost(journalpostId) } returns journalpostMedOrgnr

        steg.utfør(kontekst)

        verify {
            gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(
                journalpostId = journalpostId,
                personIdent = null,
                orgnr = "orgnr",
                beskrivelse = "tittel"
            )
        }
        verify(exactly = 0) { innkommendeJournalpostRepository.lagre(any()) }
        verify {
            avklarFordelingRepository.lagreVurdering(eq(behandlingId), withArg {
                assertThat(it.system).isEqualTo(AapSystem.IGNORERT)
            })
        }
    }
}
