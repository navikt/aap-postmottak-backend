package no.nav.aap.flyt.kontroll

import no.nav.aap.domene.behandling.Behandling
import no.nav.aap.domene.behandling.BehandlingType
import no.nav.aap.domene.behandling.StegTilstand
import no.nav.aap.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.domene.behandling.avklaringsbehov.Vurderingspunkt
import no.nav.aap.flyt.BehandlingFlyt
import no.nav.aap.flyt.Definisjon
import no.nav.aap.flyt.StegStatus
import no.nav.aap.flyt.StegType
import no.nav.aap.steg.BehandlingSteg
import no.nav.aap.steg.StartBehandlingSteg
import no.nav.aap.steg.StegInput
import no.nav.aap.steg.VurderYrkesskadeSteg

class FlytKontroller {

    var definisjoner = HashMap<BehandlingType, BehandlingFlyt>()
    var stegene = HashMap<StegType, BehandlingSteg>()

    init {
        definisjoner[BehandlingType.FØRSTEGANGSBEHANDLING] = Definisjon.førstegangsbehandling
        definisjoner[BehandlingType.REVURDERING] = Definisjon.revurdering

        // TODO: Må instansieres på en bedre måte
        Definisjon.førstegangsbehandling.stegene()
            .forEach { steg -> stegene[steg] = StartBehandlingSteg() }

        stegene[StegType.AVSLUTT_BEHANDLING] = StartBehandlingSteg()
        stegene[StegType.AVKLAR_YRKESSKADE] = VurderYrkesskadeSteg()
    }

    // Midlertidig
    var behandliger = HashMap<Long, Behandling>()

    fun prosesserBehandling(kontekst: FlytKontekst) {
        val behandling = hentBehandling(kontekst.behandlingId)
        val behandlingFlyt = definisjoner[behandling.type]!!

        var aktivtSteg = behandling.aktivtSteg()
        var nesteStegStatus = aktivtSteg.tilstand.status()
        var nesteSteg = aktivtSteg.tilstand.steg()

        var kanFortsette = true
        while (kanFortsette) {
            val avklaringsbehov = behandling.avklaringsbehov()

            val result = utførTilstandsEndring(kontekst, nesteStegStatus, avklaringsbehov, nesteSteg, behandling)

            if (result.funnetAvklaringsbehov().isNotEmpty()) {
                validerPlassering(result.funnetAvklaringsbehov(), nesteSteg, nesteStegStatus)
                behandling.leggTil(result.funnetAvklaringsbehov())
            }

            kanFortsette = result.kanFortsette()

            if (kanFortsette) {
                aktivtSteg = behandling.aktivtSteg()
                nesteStegStatus = utledNesteStegStatus(aktivtSteg)
                nesteSteg = utledNesteSteg(aktivtSteg, nesteStegStatus, behandlingFlyt)
            }
        }
    }

    private fun validerPlassering(funnetAvklaringsbehov: List<no.nav.aap.domene.behandling.avklaringsbehov.Definisjon>,
                                  nesteSteg: StegType,
                                  nesteStegStatus: StegStatus) {
        // TODO("Not yet implemented")
    }

    private fun utledNesteSteg(aktivtSteg: StegTilstand,
                               nesteStegStatus: StegStatus,
                               behandlingFlyt: BehandlingFlyt): StegType {

        if (aktivtSteg.tilstand.status() == StegStatus.AVSLUTTER && nesteStegStatus == StegStatus.START) {
            return behandlingFlyt.neste(aktivtSteg.tilstand.steg())
        }
        return aktivtSteg.tilstand.steg()
    }

    private fun utførTilstandsEndring(kontekst: FlytKontekst,
                                      nesteStegStatus: StegStatus,
                                      avklaringsbehov: List<Avklaringsbehov>,
                                      aktivtSteg: StegType,
                                      behandling: Behandling): Transisjon {
        val relevanteAvklaringsbehov = avklaringsbehov.filter { behov -> behov.definisjon.løsesISteg(aktivtSteg) }
        return when (nesteStegStatus) {
            StegStatus.INNGANG -> harAvklaringspunkt(aktivtSteg, nesteStegStatus, relevanteAvklaringsbehov)
            StegStatus.UTFØRER -> behandleSteg(aktivtSteg, kontekst)
            StegStatus.UTGANG -> harAvklaringspunkt(aktivtSteg, nesteStegStatus, relevanteAvklaringsbehov)
            StegStatus.AVSLUTTER -> harTruffetSlutten(aktivtSteg)
            else -> Fortsett
        }.also {
            val nyStegTilstand =
                StegTilstand(tilstand = no.nav.aap.flyt.Tilstand(aktivtSteg, nesteStegStatus))
            behandling.visit(nyStegTilstand)
        }
    }

    private fun harTruffetSlutten(aktivtSteg: StegType): Transisjon {
        return when (aktivtSteg) {
            StegType.AVSLUTT_BEHANDLING -> Stopp
            else -> Fortsett
        }
    }

    private fun behandleSteg(steg: StegType, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        val stegResultat = stegene.getValue(steg).utfør(input)

        return stegResultat.transisjon()
    }

    private fun harAvklaringspunkt(steg: StegType,
                                   nesteStegStatus: StegStatus,
                                   avklaringsbehov: List<Avklaringsbehov>): Transisjon {

        if (avklaringsbehov.any { behov -> behov.skalStoppeHer(steg, nesteStegStatus) }) {
            return Stopp
        }

        return Fortsett
    }

    private fun utledNesteStegStatus(aktivtSteg: StegTilstand): StegStatus {
        val status = aktivtSteg.tilstand.status()

        return StegStatus.neste(status)
    }

    // TODO: Move til tjeneste
    private fun hentBehandling(behandlingId: Long): Behandling {
        val behandling = behandliger.getOrDefault(
            behandlingId, Behandling(id = behandlingId, type = BehandlingType.FØRSTEGANGSBEHANDLING)
        )
        behandliger[behandlingId] = behandling
        return behandling
    }
}
