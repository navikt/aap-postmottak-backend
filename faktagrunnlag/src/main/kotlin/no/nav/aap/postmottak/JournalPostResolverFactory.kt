package no.nav.aap.postmottak

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.JournalpostIdResolver
import javax.sql.DataSource

fun journalpostIdMapper(referanse: Behandlingsreferanse, dataSource: DataSource) =
    dataSource.transaction(readOnly = true) {
        BehandlingRepositoryImpl(it).hent(referanse).journalpostId.referanse
    }

fun journalPostResolverFactory(dataSource: DataSource): JournalpostIdResolver<BehandlingsreferansePathParam, Unit> {
    return JournalpostIdResolver { referanse, _ ->
        journalpostIdMapper(referanse, dataSource)
    }
}
