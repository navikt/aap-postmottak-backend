package no.nav.aap.postmottak

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.tilgang.plugin.kontrakt.JournalpostIdResolver
import java.util.*
import javax.sql.DataSource

fun journalpostIdMapper(referanse: Behandlingsreferanse, dataSource: DataSource) =
    dataSource.transaction(readOnly = true) {
        BehandlingRepositoryImpl(it).hent(referanse).journalpostId.referanse
    }

fun journalpostIdFraBehandlingResolver(dataSource: DataSource): JournalpostIdResolver {
    return JournalpostIdResolver { referanse ->
        journalpostIdMapper(Behandlingsreferanse(UUID.fromString(referanse)), dataSource)
    }
}