package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StegOrkestrator::class.java)

class StegOrkestrator(private val aktivtSteg: BehandlingSteg) {

    fun utførTilstandsEndring(
        kontekst: FlytKontekst,
        nesteStegStatus: StegStatus,
        avklaringsbehov: List<Avklaringsbehov>,
        behandling: Behandling
    ): Transisjon {
        val relevanteAvklaringsbehov =
            avklaringsbehov.filter { behov -> behov.skalLøsesISteg(aktivtSteg.type()) }

        log.debug(
            "[{} - {}] Behandler steg({}) med status({})",
            kontekst.sakId,
            kontekst.behandlingId,
            aktivtSteg.type(),
            nesteStegStatus
        )
        val transisjon = when (nesteStegStatus) {
            StegStatus.UTFØRER -> behandleSteg(aktivtSteg, kontekst)
            StegStatus.AVKLARINGSPUNKT -> harAvklaringspunkt(
                aktivtSteg.type(),
                relevanteAvklaringsbehov
            )

            StegStatus.AVSLUTTER -> harTruffetSlutten(aktivtSteg.type(), behandling.flyt())
            StegStatus.TILBAKEFØRT -> behandleStegBakover(aktivtSteg, kontekst)
            else -> Fortsett
        }

        val nyStegTilstand = StegTilstand(tilstand = Tilstand(aktivtSteg.type(), nesteStegStatus))
        behandling.visit(nyStegTilstand)

        return transisjon
    }

    private fun behandleStegBakover(steg: BehandlingSteg, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        steg.vedTilbakeføring(input)

        return Fortsett
    }

    private fun harTruffetSlutten(aktivtSteg: StegType, flyt: BehandlingFlyt): Transisjon {
        return when (flyt.harTruffetSlutten(aktivtSteg)) {
            true -> Stopp
            else -> Fortsett
        }
    }

    private fun behandleSteg(steg: BehandlingSteg, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        val stegResultat = steg.utfør(input)

        return stegResultat.transisjon()
    }

    private fun harAvklaringspunkt(
        steg: StegType,
        avklaringsbehov: List<Avklaringsbehov>
    ): Transisjon {

        if (avklaringsbehov.any { behov -> behov.skalStoppeHer(steg) }) {
            return Stopp
        }

        return Fortsett
    }
}