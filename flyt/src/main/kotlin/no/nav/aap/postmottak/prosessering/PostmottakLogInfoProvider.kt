package no.nav.aap.postmottak.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.mdc.JobbLogInfoProvider
import no.nav.aap.motor.mdc.LogInformasjon

object PostmottakLogInfoProvider : JobbLogInfoProvider {

    override fun hentInformasjon(connection: DBConnection, jobbInput: JobbInput): LogInformasjon? {

        val behandlingId = jobbInput.behandlingIdOrNull() ?: return null

        val query = """
            SELECT referanse, journalpost_id
            FROM BEHANDLING 
            WHERE id = ?
        """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId)
            }
            setRowMapper { row ->
                LogInformasjon(
                    buildMap {
                        put("behandlingReferanse", row.getString("referanse"))
                        row.getStringOrNull("journalpost_id")?.let { put("journalpostId", it) }
                    }
                )
            }
        }

    }
}