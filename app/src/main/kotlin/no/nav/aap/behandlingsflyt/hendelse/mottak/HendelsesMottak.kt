package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class HendelsesMottak(private val connection: DBConnection) {
    fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        val sisteBehandling: BehandlingId? = SakHendelsesHåndterer(connection).håndtere(key, hendelse)
        if (sisteBehandling != null) {
            håndtere(sisteBehandling, hendelse.tilBehandlingHendelse())
        }
    }

    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        BehandlingHendelseHåndterer(connection).håndtere(key, hendelse)
    }
}
