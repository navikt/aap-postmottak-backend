package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

interface Grunnlag {
    fun oppdater(transaksjonsconnection: DbConnection, kontekst: FlytKontekst): Boolean
}
