package no.nav.aap.postmottak.api.auditlog

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.tilgang.auditlog.AuditLogPathParamConfig
import no.nav.aap.tilgang.auditlog.PathBrukerIdentResolver
import no.nav.aap.tilgang.plugin.kontrakt.BrukerIdentResolver
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

object DefaultAuditLogConfig {
    val auditLogger = LoggerFactory.getLogger("auditLogger")
    val app = "postmottak-backend"
    
    fun fraBehandlingPathParam(pathParam: String, transaction: DataSource) =
        AuditLogPathParamConfig(
            logger = auditLogger,
            app = app,
            brukerIdentResolver = PathBrukerIdentResolver(
                resolver = identFraBehandlingResolver(transaction),
                param = pathParam
            )
        )

    private fun hentIdentForBehandling(referanse: Behandlingsreferanse, dataSource: DataSource) =
        dataSource.transaction(readOnly = true) {
            RepositoryProvider(it).provide(JournalpostRepository::class)
                .hentHvisEksisterer(referanse)?.person?.aktivIdent()?.identifikator
                ?: throw IllegalStateException("Fant ikke person for behandling $referanse")
        }

    private fun identFraBehandlingResolver(dataSource: DataSource): BrukerIdentResolver {
        return BrukerIdentResolver { referanse ->
            hentIdentForBehandling(Behandlingsreferanse(UUID.fromString(referanse)), dataSource)
        }
    }

}

