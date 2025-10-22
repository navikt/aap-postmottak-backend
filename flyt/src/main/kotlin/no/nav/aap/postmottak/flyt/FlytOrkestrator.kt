package no.nav.aap.postmottak.flyt

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehov
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravGrunnlag
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegOrkestrator
import no.nav.aap.postmottak.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.Status
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(FlytOrkestrator::class.java)

/**
 * Har ansvar for å drive flyten til en gitt behandling. Typen behandling styrer hvilke steg som skal utføres.
 *
 * ## Forbered Behandling
 * Har ansvar for å sette behandlingen i en oppdatert tilstand i form av å innhente opplysninger for stegene man allerede
 * har prosessert og vurdere om man er nødt til å behandle steget på nytt hvis det er oppdaterte opplysninger.
 *
 * ## Prosesser Behandling
 * Har ansvar for å drive prosessen fremover, stoppe opp ved behov for besluttningsstøtte av et menneske og sørge for at
 * at stegene traverseres i den definerte rekkefølgen i flyten. Flytene defineres i typen behandlingen.
 *
 */
class FlytOrkestrator(
    private val informasjonskravGrunnlag: InformasjonskravGrunnlag,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingHendelseService: BehandlingHendelseServiceImpl,
    private val stegOrkestrator: StegOrkestrator
) {
    
    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
    ) : this(
        informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        behandlingHendelseService = BehandlingHendelseServiceImpl(repositoryProvider, gatewayProvider),
        stegOrkestrator = StegOrkestrator(repositoryProvider, gatewayProvider)
    )

    fun opprettKontekst(behandlingId: BehandlingId): FlytKontekst {
        val behandling = behandlingRepository.hent(behandlingId)

        return FlytKontekst(
            journalpostId = behandling.journalpostId,
            behandlingId = behandling.id,
            behandlingType = behandling.typeBehandling
        )
    }

    fun forberedOgProsesserBehandling(kontekst: FlytKontekst) {
        forberedBehandling(kontekst)
        prosesserBehandling(kontekst)
    }

    private fun forberedBehandling(kontekst: FlytKontekst) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovene.validateTilstand(behandling = behandling)

        val behandlingFlyt = utledFlytFra(behandling)

        behandlingFlyt.forberedFlyt(behandling.aktivtSteg())

        // fjerner av ventepunkt med utløpt frist
        if (avklaringsbehovene.erSattPåVent()) {
            val behov = avklaringsbehovene.hentVentepunkterMedUtløptFrist()
            behov.forEach { avklaringsbehovene.løsAvklaringsbehov(it.definisjon, "", SYSTEMBRUKER.ident) }
            // Hvis fortsatt på vent
            if (avklaringsbehovene.erSattPåVent()) {
                log.info("Behandlingen er på vent, skipper forberedelse.")
                return // Bail out
            } else {
                // Behandlingen er tatt av vent pga frist og flyten flyttes tilbake til steget hvor den sto på vent
                val tilbakeflyt = behandlingFlyt.tilbakeflyt(behov)
                if (!tilbakeflyt.erTom()) {
                    log.info(
                        "Tilbakeført etter tatt av vent fra '{}' til '{}'",
                        behandling.aktivtSteg(),
                        tilbakeflyt.stegene().last()
                    )
                }
                tilbakefør(kontekst, behandling, tilbakeflyt, avklaringsbehovene)
            }
        }

        val oppdaterFaktagrunnlagForKravliste =
            informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
                kravkonstruktører = behandlingFlyt.alleFaktagrunnlagFørGjeldendeSteg(),
                kontekst = kontekst
            )

        val tilbakeføringsflyt = behandlingFlyt.tilbakeflytEtterEndringer(oppdaterFaktagrunnlagForKravliste)

        if (!tilbakeføringsflyt.erTom()) {
            log.info(
                "Tilbakeført etter oppdatering av registeropplysninger fra '{}' til '{}'",
                behandling.aktivtSteg(),
                tilbakeføringsflyt.stegene().last()
            )
        }
        tilbakefør(kontekst, behandling, tilbakeføringsflyt, avklaringsbehovene)
    }

    private fun prosesserBehandling(kontekst: FlytKontekst) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovene.validateTilstand(behandling = behandling)

        // fjerner av ventepunkt med utløpt frist
        if (avklaringsbehovene.erSattPåVent()) {
            val behov = avklaringsbehovene.hentVentepunkterMedUtløptFrist()
            behov.forEach { avklaringsbehovene.løsAvklaringsbehov(it.definisjon, "", SYSTEMBRUKER.ident) }
        }

        // Hvis fortsatt på vent
        if (avklaringsbehovene.erSattPåVent()) {
            return // Bail out
        }

        val behandlingFlyt = utledFlytFra(behandling)

        var gjeldendeSteg = behandlingFlyt.forberedFlyt(behandling.aktivtSteg())

        while (true) {
            val result = stegOrkestrator.utfør(
                aktivtSteg = gjeldendeSteg,
                kontekst,
                behandling,
                behandlingFlyt.faktagrunnlagForGjeldendeSteg()
            )

            val avklaringsbehov = avklaringsbehovene.åpne()
            validerPlassering(behandlingFlyt, avklaringsbehov)

            val neste = utledNesteSteg(behandlingFlyt)

            if (!result.kanFortsette() || neste == null) {
                if (neste == null) {
                    // Avslutter behandling
                    behandlingRepository.oppdaterBehandlingStatus(
                        behandlingId = behandling.id,
                        status = Status.AVSLUTTET
                    )

                    validerAtAvklaringsBehovErLukkede(avklaringsbehovene)
                    log.info("Behandlingen har nådd slutten, avslutter behandling")
                } else {
                    // Prosessen har stoppet opp, slipp ut hendelse om at den har stoppet opp og hvorfor?
                    loggStopp(behandling, avklaringsbehovene)
                }
                val oppdatertBehandling = behandlingRepository.hent(behandling.id)
                behandlingHendelseService.stoppet(oppdatertBehandling, avklaringsbehovene)
                return
            }
            gjeldendeSteg = neste
        }
    }

    private fun validerAtAvklaringsBehovErLukkede(avklaringsbehovene: Avklaringsbehovene) {
        check(avklaringsbehovene.åpne().isEmpty()) {
            "Behandlingen er avsluttet, men det finnes åpne avklaringsbehov."
        }
    }

    private fun utledNesteSteg(
        behandlingFlyt: BehandlingFlyt
    ): FlytSteg? {
        return behandlingFlyt.neste()
    }

    internal fun forberedLøsingAvBehov(definisjoner: Definisjon, behandling: Behandling, kontekst: FlytKontekst) {
        val flyt = utledFlytFra(behandling)
        flyt.forberedFlyt(behandling.aktivtSteg())

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val behovForLøsninger = avklaringsbehovene.hentBehovForDefinisjon(definisjoner)
        val tilbakeføringsflyt = flyt.tilbakeflyt(behovForLøsninger)

        tilbakefør(
            kontekst,
            behandling,
            tilbakeføringsflyt,
            avklaringsbehovene,
            harHeltStoppet = false
        ) // Setter til false for å ikke trigge unødvendig event

        val skulleVærtISteg = flyt.skalTilStegForBehov(behovForLøsninger)
        if (skulleVærtISteg != null) {
            flyt.validerPlassering(skulleVærtISteg)
        }
    }

    private fun tilbakefør(
        kontekst: FlytKontekst,
        behandling: Behandling,
        behandlingFlyt: BehandlingFlyt,
        avklaringsbehovene: Avklaringsbehovene,
        harHeltStoppet: Boolean = true
    ) {
        if (behandlingFlyt.erTom()) {
            return
        }

        while (true) {
            val neste = behandlingFlyt.neste()

            if (neste == null) {
                loggStopp(behandling, avklaringsbehovene)
                if (harHeltStoppet) {
                    behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
                }
                return
            }
            stegOrkestrator.utførTilbakefør(
                aktivtSteg = neste,
                kontekst = kontekst,
                behandling = behandling
            )
        }
    }

    private fun loggStopp(
        behandling: Behandling,
        avklaringsbehovene: Avklaringsbehovene
    ) {
        log.info(
            "Stopper opp ved {} med {}",
            behandling.aktivtSteg(),
            avklaringsbehovene.åpne()
        )
    }

    private fun validerPlassering(
        behandlingFlyt: BehandlingFlyt,
        åpneAvklaringsbehov: List<Avklaringsbehov>
    ) {
        val nesteSteg = behandlingFlyt.aktivtStegType()
        val uhåndterteBehov = åpneAvklaringsbehov
            .filter { definisjon ->
                behandlingFlyt.erStegFør(
                    definisjon.løsesISteg(),
                    nesteSteg
                )
            }
        check(uhåndterteBehov.isEmpty()) { "Har uhåndterte behov som skulle vært håndtert før nåværende steg = '$nesteSteg'" }
    }

    private fun utledFlytFra(behandling: Behandling) = utledType(behandling.typeBehandling).flyt()

}
