package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.verdityper.Periode

data class Årsak(val type: EndringType, val periode: Periode? = null)
