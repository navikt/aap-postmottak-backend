package no.nav.aap.behandlingsflyt.hendelse.mottak.dokument

import no.nav.aap.behandlingsflyt.Periode

interface PeriodisertData {
    fun periode(): Periode
}