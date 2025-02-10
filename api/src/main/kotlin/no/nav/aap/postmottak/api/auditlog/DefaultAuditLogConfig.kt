package no.nav.aap.postmottak.api.auditlog

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.tilgang.auditlog.AuditLogPathParamConfig
import no.nav.aap.tilgang.auditlog.PathBrukerIdentResolver
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

object DefaultAuditLogConfig {
    val auditLogger = LoggerFactory.getLogger("auditLogger")
    val app = requiredConfigForKey("nais.app.name")

    fun fraBehandlingPathParam(pathParam: String, dataSource: DataSource) =
        AuditLogPathParamConfig(
            logger = auditLogger,
            app = app,
            brukerIdentResolver = PathBrukerIdentResolver(
                resolver = { referanse ->
                    hentIdentForBehandling(Behandlingsreferanse(UUID.fromString(referanse)), dataSource)
                },
                param = pathParam
            )
        )

    fun fraJournalpostPathParam(pathParam: String, dataSource: DataSource) =
        AuditLogPathParamConfig(
            logger = auditLogger,
            app = app,
            brukerIdentResolver = PathBrukerIdentResolver(
                resolver = { referanse ->
                    hentIdentForJournalpost(JournalpostId(referanse.toLong()), dataSource)
                },
                param = pathParam
            )
        )

    private fun hentIdentForBehandling(referanse: Behandlingsreferanse, dataSource: DataSource) =
        dataSource.transaction(readOnly = true) {
            RepositoryProvider(it).provide(JournalpostRepository::class)
                .hentHvisEksisterer(referanse)?.person?.aktivIdent()?.identifikator
                ?: throw IllegalStateException("Fant ikke person for behandling $referanse")
        }

    private fun hentIdentForJournalpost(referanse: JournalpostId, dataSource: DataSource) =
        dataSource.transaction(readOnly = true) {
            RepositoryProvider(it).provide(JournalpostRepository::class)
                .hentHvisEksisterer(referanse)?.person?.aktivIdent()?.identifikator
                ?: throw IllegalStateException("Fant ikke person for journalpost $referanse")
        }
    
}

