package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection

interface FlytSteg {
    fun konstruer(connection: DbConnection): BehandlingSteg

    fun type(): StegType
}