package no.nav.aap.hendelse.mottak

import no.nav.aap.domene.Periode

class DokumentMottattPersonHendelse(private val periode: Periode) : PersonHendelse {

    override fun periode(): Periode {
        return periode
    }

    override fun tilSakshendelse(): SakHendelse {
        return DokumentMottattSakHendelse()
    }
}
