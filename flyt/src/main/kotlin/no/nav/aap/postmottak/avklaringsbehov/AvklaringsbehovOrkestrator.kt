package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.postmottak.avklaringsbehov.løsning.SattPåVentLøsning
import no.nav.aap.postmottak.flyt.FlytOrkestrator
import no.nav.aap.postmottak.flyt.utledType
import no.nav.aap.postmottak.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.postmottak.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import org.slf4j.LoggerFactory

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

    fun taAvVentHvisPåVentOgFortsettProsessering(behandlingId: BehandlingId) {
        val behandling = behandlingRepository.hent(behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.validateTilstand(behandling = behandling)

        val kontekst = behandling.flytKontekst()
        if (avklaringsbehovene.erSattPåVent()) {
            løsAvklaringsbehov(
                kontekst = kontekst,
                avklaringsbehovene = avklaringsbehovene,
                avklaringsbehov = SattPåVentLøsning(),
                bruker = SYSTEMBRUKER,
                behandling = behandling
            )
        }
        fortsettProsessering(kontekst)
    }

    fun løsAvklaringsbehovOgFortsettProsessering(
        kontekst: FlytKontekst,
        avklaringsbehov: AvklaringsbehovLøsning,
        ingenEndringIGruppe: Boolean,
        bruker: Bruker
    ) {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        løsAvklaringsbehov(
            kontekst, avklaringsbehovene, avklaringsbehov, bruker, behandling
        )
        markerAvklaringsbehovISammeGruppeForLøst(
            kontekst, ingenEndringIGruppe, avklaringsbehovene, bruker
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

    private fun markerAvklaringsbehovISammeGruppeForLøst(
        kontekst: FlytKontekst, ingenEndringIGruppe: Boolean, avklaringsbehovene: Avklaringsbehovene, bruker: Bruker
    ) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        if (ingenEndringIGruppe && avklaringsbehovene.harVærtSendtTilbakeFraBeslutterTidligere()) {
            val typeBehandling = behandling.typeBehandling
            val flyt = utledType(typeBehandling).flyt()

            flyt.forberedFlyt(behandling.aktivtSteg())
            val gjenståendeStegIGruppe = flyt.gjenståendeStegIAktivGruppe()

            val behovSomSkalSettesTilAvsluttet = avklaringsbehovene.alle()
                .filter { behov -> gjenståendeStegIGruppe.any { stegType -> behov.løsesISteg() == stegType } }
            log.info("Markerer påfølgende avklaringsbehov[${behovSomSkalSettesTilAvsluttet}] på behandling[${behandling.referanse}] som avsluttet")

            behovSomSkalSettesTilAvsluttet.forEach { avklaringsbehovene.ingenEndring(it, bruker.ident) }
        }
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
        kontekst: FlytKontekst, avklaringsbehovene: Avklaringsbehovene, it: AvklaringsbehovLøsning, bruker: Bruker
    ) {
        avklaringsbehovene.leggTilFrivilligHvisMangler(it.definisjon(), bruker)
        val løsningsResultat = it.løs(repositoryProvider, gatewayProvider,AvklaringsbehovKontekst(bruker, kontekst))

        avklaringsbehovene.løsAvklaringsbehov(
            it.definisjon(), løsningsResultat.begrunnelse, bruker.ident, løsningsResultat.kreverToTrinn
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
}
