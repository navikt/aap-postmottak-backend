package no.nav.aap.domene.behandling

import no.nav.aap.domene.Periode

data class Årsak(val type: EndringType, val periode: Periode? = null) {

}
