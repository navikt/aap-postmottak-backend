package no.nav.aap.postmottak.flyt.steg

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravGrunnlag
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.flyt.steg.internal.StegKonstruktørImpl
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.StegTilstand
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.flyt.StegStatus
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StegOrkestrator::class.java)

/**
 * Håndterer den definerte prosessen i et gitt steg, flytter behandlingen gjennom de forskjellige fasene internt i et
 * steg. Et steg beveger seg gjennom flere faser som har forskjellig ansvar.
 *
 * @see no.nav.aap.postmottak.journalpostogbehandling.flyt.StegStatus.START:            Teknisk markør for at flyten har flyttet seg til et gitt steg
 *
 * @see no.nav.aap.postmottak.journalpostogbehandling.flyt.StegStatus.UTFØRER:          Utfører forrettningslogikken i steget ved å kalle på
 * @see no.nav.aap.postmottak.flyt.steg.BehandlingSteg.utfør
 *
 * @see no.nav.aap.postmottak.journalpostogbehandling.flyt.StegStatus.AVKLARINGSPUNKT:  Vurderer om maskinen har bedt om besluttningstøtte fra
 * et menneske og stopper prosessen hvis det er et punkt som krever stopp i dette steget.
 *
 * @see no.nav.aap.postmottak.journalpostogbehandling.flyt.StegStatus:        Teknisk markør for avslutting av steget
 */
class StegOrkestrator(
    private val informasjonskravGrunnlag: InformasjonskravGrunnlag,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val stegKonstruktør: StegKonstruktør
) {

    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
    ) : this(
        informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(repositoryProvider, gatewayProvider),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        stegKonstruktør = StegKonstruktørImpl(repositoryProvider, gatewayProvider)
    )

    fun utfør(
        aktivtSteg: FlytSteg,
        kontekst: FlytKontekst,
        behandling: Behandling,
        faktagrunnlagForGjeldendeSteg: List<Pair<StegType, Informasjonskravkonstruktør>>
    ): Transisjon {
        var gjeldendeStegStatus = StegStatus.START
        log.info("Behandler steg '{}'", aktivtSteg.type())

        while (true) {
            val resultat = utførTilstandsEndring(
                aktivtSteg,
                kontekst,
                gjeldendeStegStatus,
                behandling,
                faktagrunnlagForGjeldendeSteg
            )
            if (gjeldendeStegStatus in setOf(StegStatus.START, StegStatus.OPPDATER_FAKTAGRUNNLAG)) {
                // Legger denne her slik at vi får savepoint på at vi har byttet steg, slik at vi starter opp igjen på rett sted når prosessen dras i gang igjen
                behandlingRepository.markerSavepoint()
            }

            if (gjeldendeStegStatus == StegStatus.AVSLUTTER) {
                return resultat
            }

            if (!resultat.kanFortsette()) {
                return resultat
            }
            gjeldendeStegStatus = gjeldendeStegStatus.neste()
        }
    }

    fun utførTilbakefør(
        aktivtSteg: FlytSteg,
        kontekst: FlytKontekst,
        behandling: Behandling
    ): Transisjon {
        return utførTilstandsEndring(aktivtSteg, kontekst, StegStatus.TILBAKEFØRT, behandling, listOf())
    }

    private fun utførTilstandsEndring(
        aktivtSteg: FlytSteg,
        kontekst: FlytKontekst,
        nesteStegStatus: StegStatus,
        behandling: Behandling,
        faktagrunnlagForGjeldendeSteg: List<Pair<StegType, Informasjonskravkonstruktør>>
    ): Transisjon {
        val behandlingSteg = stegKonstruktør.konstruer(aktivtSteg)

        log.debug(
            "Behandler steg({}) med status({})",
            aktivtSteg.type(),
            nesteStegStatus
        )
        val transisjon = when (nesteStegStatus) {
            StegStatus.START -> Fortsett
            StegStatus.OPPDATER_FAKTAGRUNNLAG -> oppdaterFaktagrunnlag(kontekst, faktagrunnlagForGjeldendeSteg)
            StegStatus.UTFØRER -> behandleSteg(aktivtSteg, behandlingSteg, kontekst)
            StegStatus.AVKLARINGSPUNKT -> harAvklaringspunkt(aktivtSteg, kontekst.behandlingId)
            StegStatus.TILBAKEFØRT -> behandleStegBakover(kontekst, behandlingSteg)
            StegStatus.AVSLUTTER -> Fortsett
        }

        val nyStegTilstand = StegTilstand(stegType = aktivtSteg.type(), stegStatus = nesteStegStatus)
        loggStegHistorikk(behandling, nyStegTilstand)

        return transisjon
    }

    private fun oppdaterFaktagrunnlag(
        kontekst: FlytKontekst,
        faktagrunnlagForGjeldendeSteg: List<Pair<StegType, Informasjonskravkonstruktør>>
    ): Fortsett {
        informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
            faktagrunnlagForGjeldendeSteg,
            kontekst
        )
        return Fortsett
    }

    private fun behandleSteg(
        aktivtSteg: FlytSteg,
        behandlingSteg: BehandlingSteg,
        kontekst: FlytKontekst
    ): Transisjon {
        val stegResultat = behandlingSteg.utfør(kontekst)

        val resultat = stegResultat.transisjon()

        if (resultat is FunnetAvklaringsbehov) {
            log.info(
                "Fant avklaringsbehov: {}",
                resultat.avklaringsbehov()
            )
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            avklaringsbehovene.leggTil(resultat.avklaringsbehov(), aktivtSteg.type())
        } else if (resultat is FunnetVentebehov) {
            log.info(
                "Fant ventebehov: {}",
                resultat.ventebehov()
            )
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            resultat.ventebehov().forEach {
                avklaringsbehovene.leggTil(
                    definisjoner = listOf(it.definisjon),
                    stegType = aktivtSteg.type(),
                    frist = it.frist,
                    grunn = it.grunn
                )
            }
        } else if (resultat is Fortsett) {
            // Avbryt eksisterende ventebehov
            avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                .hentVentepunkter().forEach {
                    log.info("Avbryter ventebehov: {}", it)
                    it.løs("Har gått videre til nytt steg", SYSTEMBRUKER.ident)
                }
        }

        return resultat
    }

    private fun harAvklaringspunkt(
        aktivtSteg: FlytSteg,
        behandlingId: BehandlingId
    ): Transisjon {
        val relevanteAvklaringsbehov =
            avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId).alle()
                .filter { it.erÅpent() }
                .filter { behov -> behov.skalLøsesISteg(aktivtSteg.type()) }

        if (relevanteAvklaringsbehov.any { behov -> behov.skalStoppeHer(aktivtSteg.type()) }) {
            return Stopp
        }

        return Fortsett
    }

    private fun behandleStegBakover(
        kontekst: FlytKontekst, behandlingSteg: BehandlingSteg,
    ): Transisjon {
        behandlingSteg.vedTilbakeføring(kontekst)
        return Fortsett
    }

    private fun loggStegHistorikk(
        behandling: Behandling,
        nyStegTilstand: StegTilstand
    ) {
        val førStatus = behandling.status()
        behandling.visit(nyStegTilstand)
        behandlingRepository.loggBesøktSteg(behandlingId = behandling.id, nyStegTilstand)
        val etterStatus = nyStegTilstand.steg().status
        if (førStatus != etterStatus) {
            behandlingRepository.oppdaterBehandlingStatus(behandlingId = behandling.id, status = etterStatus)
        }
    }
}
