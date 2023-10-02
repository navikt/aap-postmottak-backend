package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.domene.behandling.EndringType
import no.nav.aap.behandlingsflyt.domene.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import java.util.*

/**
 * Holder styr på den definerte behandlingsflyten og regner ut hvilket steg det skal flyttes
 */
class BehandlingFlyt(
    private var flyt: List<BehandlingSteg>,
    private var endringTilSteg: Map<EndringType, no.nav.aap.behandlingsflyt.flyt.StegType>
) {

    fun utledNesteSteg(aktivtSteg: StegTilstand, nesteStegStatus: no.nav.aap.behandlingsflyt.flyt.StegStatus): BehandlingSteg? {
        if (aktivtSteg.tilstand.status() == no.nav.aap.behandlingsflyt.flyt.StegStatus.AVSLUTTER && nesteStegStatus == no.nav.aap.behandlingsflyt.flyt.StegStatus.START) {
            return neste(aktivtSteg.tilstand.steg())
        }
        return steg(aktivtSteg.tilstand.steg())
    }

    /**
     * Finner neste steget som skal prosesseres etter at nåværende er ferdig
     */
    fun neste(nåværendeSteg: no.nav.aap.behandlingsflyt.flyt.StegType): BehandlingSteg? {
        val nåværendeIndex = this.flyt.indexOfFirst { it.type() == nåværendeSteg }
        if (nåværendeIndex == -1) {
            throw IllegalStateException("[Utvikler feil] Nåværende steg '$nåværendeSteg' er ikke en del av den definerte prosessen")
        }

        val iterator = this.flyt.listIterator(nåværendeIndex)
        iterator.next() // Er alltid nåværende steg

        if (iterator.hasNext()) {
            return iterator.next()
        }

        return null
    }

    /**
     * Avgjør hvilket steg prosessen skal fortsette fra. Hvis ingen endring så står pekeren stille
     */
    fun nesteEtterEndringer(nåværendeSteg: no.nav.aap.behandlingsflyt.flyt.StegType, vararg endringer: EndringType): BehandlingSteg {
        if (endringer.isNotEmpty()) {
            val nåværendeIndex = flyt.indexOfFirst { it.type() == nåværendeSteg }
            if (nåværendeIndex == -1) {
                throw IllegalStateException("[Utvikler feil] Nåværende steg '" + nåværendeSteg + "' er ikke en del av den definerte prosessen")
            }

            val endringsIndex =
                endringer.map { endring -> flyt.indexOfFirst { it.type() == endringTilSteg[endring] } }.min()

            if (endringsIndex < nåværendeIndex) {
                return flyt[endringsIndex]
            }
        }
        return flyt[flyt.indexOfFirst { it.type() == nåværendeSteg }]
    }

    fun steg(nåværendeSteg: no.nav.aap.behandlingsflyt.flyt.StegType): BehandlingSteg {
        return flyt[flyt.indexOfFirst { it.type() == nåværendeSteg }]
    }

    fun erStegFør(stegA: no.nav.aap.behandlingsflyt.flyt.StegType, stegB: no.nav.aap.behandlingsflyt.flyt.StegType): Boolean {
        val aIndex = flyt.indexOfFirst { it.type() == stegA }
        val bIndex = flyt.indexOfFirst { it.type() == stegB }

        return aIndex < bIndex
    }

    fun compareable(): no.nav.aap.behandlingsflyt.flyt.StegComparator {
        return no.nav.aap.behandlingsflyt.flyt.StegComparator(flyt)
    }

    fun erStegFørEllerLik(stegA: no.nav.aap.behandlingsflyt.flyt.StegType, stegB: no.nav.aap.behandlingsflyt.flyt.StegType): Boolean {
        val aIndex = flyt.indexOfFirst { it.type() == stegA }
        val bIndex = flyt.indexOfFirst { it.type() == stegB }

        return aIndex <= bIndex
    }

    fun forrige(nåværendeSteg: no.nav.aap.behandlingsflyt.flyt.StegType): BehandlingSteg? {
        val nåværendeIndex = flyt.indexOfFirst { it.type() == nåværendeSteg }

        if (nåværendeIndex == -1) {
            throw IllegalStateException("[Utvikler feil] '$nåværendeSteg' er ikke en del av den definerte prosessen")
        }

        val iterator = this.flyt.listIterator(nåværendeIndex)
        if (iterator.hasPrevious()) {
            return iterator.previous()
        }

        return null
    }

    fun stegene(): List<no.nav.aap.behandlingsflyt.flyt.StegType> {
        return flyt.map { it.type() }
    }

    fun harTruffetSlutten(nåværendeSteg: no.nav.aap.behandlingsflyt.flyt.StegType): Boolean {
        return flyt.indexOfFirst { it.type() == nåværendeSteg } == (flyt.size - 1)
    }
}

class StegComparator(private var flyt: List<BehandlingSteg>) : Comparator<no.nav.aap.behandlingsflyt.flyt.StegType> {
    override fun compare(stegA: no.nav.aap.behandlingsflyt.flyt.StegType?, stegB: no.nav.aap.behandlingsflyt.flyt.StegType?): Int {
        val aIndex = flyt.indexOfFirst { it.type() == stegA }
        val bIndex = flyt.indexOfFirst { it.type() == stegB }

        return aIndex.compareTo(bIndex)
    }

}

class BehandlingFlytBuilder {
    private var flyt: MutableList<BehandlingSteg> = mutableListOf()
    private var endringTilSteg: MutableMap<EndringType, no.nav.aap.behandlingsflyt.flyt.StegType> = mutableMapOf()
    private var buildt = false

    fun medSteg(steg: BehandlingSteg, vararg endringer: EndringType): no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder {
        if (buildt) {
            throw IllegalStateException("[Utvikler feil] Builder er allerede bygget")
        }
        if (no.nav.aap.behandlingsflyt.flyt.StegType.UDEFINERT == steg.type()) {
            throw IllegalStateException("[Utvikler feil] StegType UDEFINERT er ugyldig å legge til i flyten")
        }
        this.flyt.add(steg)
        endringer.forEach { endring ->
            this.endringTilSteg[endring] = steg.type()
        }
        return this
    }

    fun build(): no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt {
        if (buildt) {
            throw IllegalStateException("[Utvikler feil] Builder er allerede bygget")
        }
        buildt = true

        return no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt(
            Collections.unmodifiableList(flyt),
            Collections.unmodifiableMap(endringTilSteg)
        )
    }
}
