package no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.søknad

import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.PeriodisertData
import no.nav.aap.verdityper.Periode

class Søknad(private val periode: Periode) : PeriodisertData {
    override fun periode(): Periode {
        return periode
    }
}