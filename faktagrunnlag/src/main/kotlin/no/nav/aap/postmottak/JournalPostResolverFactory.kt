package no.nav.aap.postmottak

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.tilgang.JournalpostIdResolver
import javax.sql.DataSource

fun journalPostResolverFactory(dataSource: DataSource): JournalpostIdResolver<Behandlingsreferanse, Unit> {
    return JournalpostIdResolver { referanse, _ ->
        dataSource.transaction(readOnly = true) { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            behandlingRepository.hent(referanse).journalpostId.referanse
        }
    }
}

