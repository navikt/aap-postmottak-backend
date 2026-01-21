package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.postmottak.flyt.FlytOrkestrator
import no.nav.aap.postmottak.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.postmottak.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import org.slf4j.LoggerFactory
import java.time.LocalDate

class AvklaringsbehovOrkestrator(
    private val repositoryProvider: RepositoryProvider,
    private val behandlingHendelseService: BehandlingHendelseServiceImpl,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val flytOrkestrator: FlytOrkestrator,
    private val gatewayProvider: GatewayProvider
) {

    private val log = LoggerFactory.getLogger(AvklaringsbehovOrkestrator::class.java)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        repositoryProvider,
        behandlingHendelseService = BehandlingHendelseServiceImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        flytJobbRepository = repositoryProvider.provide(),
        flytOrkestrator = FlytOrkestrator(repositoryProvider, gatewayProvider),
        gatewayProvider
    )

    fun løsAvklaringsbehovOgFortsettProsessering(
        kontekst: FlytKontekst,
        avklaringsbehov: AvklaringsbehovLøsning,
        bruker: Bruker
    ) {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        løsAvklaringsbehov(
            kontekst, avklaringsbehovene, avklaringsbehov, bruker, behandling
        )

        fortsettProsessering(kontekst)
    }

    private fun fortsettProsessering(kontekst: FlytKontekst) {
        flytJobbRepository.leggTil(
            JobbInput(jobb = ProsesserBehandlingJobbUtfører).forBehandling(
                kontekst.journalpostId.referanse,
                kontekst.behandlingId.id
            ).medCallId()
        )
    }

    private fun løsAvklaringsbehov(
        kontekst: FlytKontekst,
        avklaringsbehovene: Avklaringsbehovene,
        avklaringsbehov: AvklaringsbehovLøsning,
        bruker: Bruker,
        behandling: Behandling
    ) {
        val definisjoner = avklaringsbehov.definisjon()
        log.info("Forsøker løse avklaringsbehov[${definisjoner}] på behandling[${behandling.referanse}]")

        avklaringsbehovene.validateTilstand(
            behandling = behandling, avklaringsbehov = definisjoner
        )

        // løses det behov som fremtvinger tilbakehopp?
        flytOrkestrator.forberedLøsingAvBehov(definisjoner, behandling, kontekst)

        // Bør ideelt kalle på
        løsFaktiskAvklaringsbehov(kontekst, avklaringsbehovene, avklaringsbehov, bruker)
    }

    private fun løsFaktiskAvklaringsbehov(
        kontekst: FlytKontekst,
        avklaringsbehovene: Avklaringsbehovene,
        avklaringsbehovLøsning: AvklaringsbehovLøsning,
        bruker: Bruker
    ) {
        avklaringsbehovene.leggTilFrivilligHvisMangler(avklaringsbehovLøsning.definisjon(), bruker)
        val løsningsResultat =
            avklaringsbehovLøsning.løs(repositoryProvider, gatewayProvider, AvklaringsbehovKontekst(bruker, kontekst))

        avklaringsbehovene.løsAvklaringsbehov(
            avklaringsbehovLøsning.definisjon(),
            løsningsResultat.begrunnelse,
            bruker.ident
        )
    }

    fun settBehandlingPåVent(behandlingId: BehandlingId, hendelse: BehandlingSattPåVent) {
        val behandling = behandlingRepository.hent(behandlingId)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        avklaringsbehovene.validateTilstand(behandling = behandling)

        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.MANUELT_SATT_PÅ_VENT),
            stegType = behandling.aktivtSteg(),
            frist = hendelse.frist,
            begrunnelse = hendelse.begrunnelse,
            grunn = hendelse.grunn,
            bruker = hendelse.bruker
        )

        avklaringsbehovene.validateTilstand(behandling = behandling)
        avklaringsbehovene.validerPlassering(behandling = behandling)
        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
    }

    fun settBehandlingPåVentForTemaEndring(behandlingId: BehandlingId) {
        val behandling = behandlingRepository.hent(behandlingId)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        avklaringsbehovene.validateTilstand(behandling = behandling)

        avklaringsbehovene.leggTil(
            definisjoner = listOf(Definisjon.VENT_PA_GOSYS),
            stegType = behandling.aktivtSteg(),
            frist = LocalDate.now().plusWeeks(2),
            begrunnelse = "Venter på behandling i Gosys.",
            grunn = ÅrsakTilSettPåVent.VENTER_PÅ_BEHANDLING_I_GOSYS,
            bruker = SYSTEMBRUKER
        )

        avklaringsbehovene.validateTilstand(behandling = behandling)
        avklaringsbehovene.validerPlassering(behandling = behandling)
        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
    }

    fun taAvVentPgaGosys(behandlingId: BehandlingId) {
        log.info("Tar av vent for behandling $behandlingId")
        val behandling = behandlingRepository.hent(behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        avklaringsbehovene.validateTilstand(behandling = behandling)

        if (avklaringsbehovene.hentVentepunkter().map { it.definisjon }.contains(Definisjon.VENT_PA_GOSYS)) {
            avklaringsbehovene.løsAvklaringsbehov(
                definisjon = Definisjon.VENT_PA_GOSYS,
                begrunnelse = "Ny oppdatering på journalpost.",
                endretAv = SYSTEMBRUKER.ident,
            )
        }

        avklaringsbehovene.validateTilstand(behandling = behandling)
        avklaringsbehovene.validerPlassering(behandling = behandling)
        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
    }
}
