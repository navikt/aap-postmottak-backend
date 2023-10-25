package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.Periode

class DokumentMottattPersonHendelse(private val periode: Periode) : PersonHendelse {

    override fun periode(): Periode {
        return periode
    }

    override fun tilSakshendelse(): SakHendelse {
        return DokumentMottattSakHendelse()
    }
}
