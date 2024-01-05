package no.nav.aap.behandlingsflyt.hendelse.mottak.dokument

import no.nav.aap.verdityper.Periode

interface PeriodisertData {
    fun periode(): Periode
}