package no.nav.aap.domene.behandling

import no.nav.aap.domene.typer.Periode

data class Årsak(val type: EndringType, val periode: Periode? = null) {

}
