package no.nav.aap.postmottak.flyt.internals

import no.nav.aap.postmottak.hendelse.mottak.SakHendelse
import no.nav.aap.verdityper.Periode

interface PersonHendelse {

    fun periode(): Periode

    fun tilSakshendelse(): SakHendelse
}
