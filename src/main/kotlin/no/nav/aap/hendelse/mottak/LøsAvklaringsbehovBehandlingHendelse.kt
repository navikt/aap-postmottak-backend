package no.nav.aap.hendelse.mottak

import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning

class LøsAvklaringsbehovBehandlingHendelse(private val løsning: AvklaringsbehovLøsning, private val versjon: Long) :
    BehandlingHendelse {

    fun behov(): AvklaringsbehovLøsning {
        return løsning
    }

    fun versjon(): Long {
        return versjon
    }
}
