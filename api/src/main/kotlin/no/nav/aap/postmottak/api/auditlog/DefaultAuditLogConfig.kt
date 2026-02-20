package no.nav.aap.postmottak.api.auditlog

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.tilgang.auditlog.AuditLogPathParamConfig
import no.nav.aap.tilgang.auditlog.PathBrukerIdentResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

object DefaultAuditLogConfig {
    val auditLogger: Logger = LoggerFactory.getLogger("auditLogger")
    val app = requiredConfigForKey("nais.app.name")

    fun fraBehandlingPathParam(pathParam: String, dataSource: DataSource, repositoryRegistry: RepositoryRegistry) =
        AuditLogPathParamConfig(
            logger = auditLogger,
            app = app,
            brukerIdentResolver = PathBrukerIdentResolver(
                resolver = { referanse ->
                    hentIdentForBehandling(
                        Behandlingsreferanse(UUID.fromString(referanse)),
                        dataSource,
                        repositoryRegistry
                    )
                },
                param = pathParam
            )
        )

    fun fraJournalpostPathParam(pathParam: String, dataSource: DataSource, repositoryRegistry: RepositoryRegistry) =
        AuditLogPathParamConfig(
            logger = auditLogger,
            app = app,
            brukerIdentResolver = PathBrukerIdentResolver(
                resolver = { referanse ->
                    hentIdentForJournalpost(JournalpostId(referanse.toLong()), dataSource, repositoryRegistry)
                },
                param = pathParam
            )
        )

    private fun hentIdentForBehandling(
        referanse: Behandlingsreferanse,
        dataSource: DataSource,
        repositoryRegistry: RepositoryRegistry
    ) =
        dataSource.transaction(readOnly = true) {
            repositoryRegistry.provider(it).provide(JournalpostRepository::class)
                .hentHvisEksisterer(referanse)?.person?.aktivIdent()?.identifikator
                ?: throw IllegalStateException("Fant ikke person for behandling $referanse")
        }

    private fun hentIdentForJournalpost(
        referanse: JournalpostId,
        dataSource: DataSource,
        repositoryRegistry: RepositoryRegistry
    ) =
        dataSource.transaction(readOnly = true) {
            repositoryRegistry.provider(it).provide(JournalpostRepository::class)
                .hentHvisEksisterer(referanse)?.person?.aktivIdent()?.identifikator
                ?: throw IllegalStateException("Fant ikke person for journalpost $referanse")
        }

}

