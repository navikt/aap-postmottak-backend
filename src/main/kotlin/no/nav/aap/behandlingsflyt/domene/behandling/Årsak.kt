package no.nav.aap.behandlingsflyt.domene.behandling

import no.nav.aap.behandlingsflyt.domene.Periode

data class Årsak(val type: EndringType, val periode: Periode? = null)
