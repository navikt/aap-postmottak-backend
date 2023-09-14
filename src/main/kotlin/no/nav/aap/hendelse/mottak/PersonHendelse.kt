package no.nav.aap.hendelse.mottak

import no.nav.aap.domene.Periode

interface PersonHendelse {

    fun periode(): Periode

    fun tilSakshendelse(): SakHendelse
}
