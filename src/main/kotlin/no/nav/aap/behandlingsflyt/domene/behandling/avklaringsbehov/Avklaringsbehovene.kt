package no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.domene.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.StegType

class Avklaringsbehovene {

    private val avklaringsbehovene: MutableList<Avklaringsbehov> = mutableListOf()

    fun leggTil(funnetAvklaringsbehov: List<Definisjon>, steg: StegType) {
        funnetAvklaringsbehov.stream()
            .map { definisjon ->
                Avklaringsbehov(
                    definisjon,
                    funnetISteg = steg
                )
            }
            .forEach { this.leggTil(it) }
    }

    fun leggTil(avklaringsbehov: Avklaringsbehov) {
        val relevantBehov = avklaringsbehovene.firstOrNull { it.definisjon == avklaringsbehov.definisjon }

        if (relevantBehov != null) {
            relevantBehov.reåpne()
        } else {
            avklaringsbehovene.add(avklaringsbehov)
        }
    }

    fun løsAvklaringsbehov(definisjon: Definisjon, begrunnelse: String, endretAv: String) {
        avklaringsbehovene.single { it.definisjon == definisjon }.løs(begrunnelse, endretAv = endretAv)
    }

    fun alle(): List<Avklaringsbehov> = avklaringsbehovene.toList()
    fun åpne(): List<Avklaringsbehov> = avklaringsbehovene.filter { it.erÅpent() }.toList()

    fun skalHoppesTilbake(
        behandlingFlyt: no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt,
        aktivtSteg: StegTilstand,
        definisjoner: List<Definisjon>
    ): Boolean {
        val relevanteBehov = avklaringsbehovene.filter { it.definisjon in definisjoner }

        return relevanteBehov.any { behov ->
            behandlingFlyt.erStegFør(
                stegA = behov.løsesISteg(),
                stegB = aktivtSteg.tilstand.steg()
            )
        }
    }
}
