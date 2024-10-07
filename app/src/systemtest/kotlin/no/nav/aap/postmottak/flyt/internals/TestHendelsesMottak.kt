package no.nav.aap.postmottak.flyt.internals

import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingHendelseHåndterer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.hendelse.mottak.BehandlingHendelse
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import javax.sql.DataSource

class TestHendelsesMottak(private val dataSource: DataSource) {
    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        dataSource.transaction { connection ->
            BehandlingHendelseHåndterer(connection).håndtere(key, hendelse)
        }
    }
}
