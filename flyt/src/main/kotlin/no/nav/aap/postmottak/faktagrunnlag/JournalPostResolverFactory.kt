package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.tilgang.plugin.kontrakt.JournalpostIdResolver
import java.util.*
import javax.sql.DataSource

fun journalpostIdMapper(referanse: Behandlingsreferanse, dataSource: DataSource) =
    dataSource.transaction(readOnly = true) {
        RepositoryProvider(it).provide(BehandlingRepository::class).hent(referanse).journalpostId.referanse
    }

fun journalpostIdFraBehandlingResolver(dataSource: DataSource): JournalpostIdResolver {
    return JournalpostIdResolver { referanse ->
        journalpostIdMapper(Behandlingsreferanse(UUID.fromString(referanse)), dataSource)
    }
}