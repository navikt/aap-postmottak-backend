package no.nav.aap.postmottak.flyt.internals

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.hendelse.mottak.BehandlingHendelse
import no.nav.aap.postmottak.hendelse.mottak.BehandlingHendelseHåndterer
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import javax.sql.DataSource

class TestHendelsesMottak(
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider
) {
    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        dataSource.transaction { connection ->
            BehandlingHendelseHåndterer(repositoryRegistry.provider(connection), gatewayProvider).håndtere(
                key,
                hendelse
            )
        }
    }
}
