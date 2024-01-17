package no.nav.aap.behandlingsflyt.hendelse.mottak.dokument

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.verdityper.Periode

class StrukturertDokument<T : PeriodisertData>(val data: T, val brevkode: Brevkode) {

    fun periode(): Periode {
        return data.periode()
    }
}