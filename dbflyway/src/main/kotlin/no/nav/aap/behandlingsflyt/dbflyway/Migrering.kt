package no.nav.aap.behandlingsflyt.dbflyway

import org.flywaydb.core.Flyway
import javax.sql.DataSource

object Migrering {
    fun migrate(dataSource: DataSource) {
        val miljø = Miljø.er()
        val flyway = Flyway
            .configure()
            .cleanDisabled(false)
            .cleanOnValidationError(true)
            .dataSource(dataSource)
            .locations("flyway")
            .validateMigrationNaming(true)
            .load()

        flyway.migrate()
    }

}