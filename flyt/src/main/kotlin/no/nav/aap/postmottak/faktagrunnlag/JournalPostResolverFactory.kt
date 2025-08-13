package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.tilgang.plugin.kontrakt.JournalpostIdResolver
import java.util.*
import javax.sql.DataSource

fun journalpostIdMapper(referanse: Behandlingsreferanse, repositoryRegistry: RepositoryRegistry, dataSource: DataSource) =
    dataSource.transaction(readOnly = true) {
        repositoryRegistry.provider(it).provide(BehandlingRepository::class).hent(referanse).journalpostId.referanse
    }

fun journalpostIdFraBehandlingResolver(repositoryRegistry: RepositoryRegistry, dataSource: DataSource): JournalpostIdResolver {
    return JournalpostIdResolver { referanse ->
        journalpostIdMapper(Behandlingsreferanse(UUID.fromString(referanse)), repositoryRegistry, dataSource)
    }
}