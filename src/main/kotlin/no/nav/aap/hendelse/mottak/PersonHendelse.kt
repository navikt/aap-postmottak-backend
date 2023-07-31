package no.nav.aap.hendelse.mottak

import no.nav.aap.domene.typer.Periode

interface PersonHendelse {

    fun periode(): Periode

    fun tilSakshendelse(): SakHendelse
}
