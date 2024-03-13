package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.PeriodisertData
import no.nav.aap.verdityper.Periode

class Søknad(@JsonProperty("periode") val periode: Periode, @JsonProperty("student") val student: Boolean) :
    PeriodisertData {
    override fun periode(): Periode {
        return periode
    }
}