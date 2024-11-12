package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.DBConnection

data class JoarkHendelse(
    val key: String,
    val value: String
)

class HendelsesRepository(private val connection: DBConnection) {

    fun hentHendelse(hendelsesId: HendelsesId): JoarkHendelse {
        return connection.queryFirst("""
           SELECT * FROM JOARK_HENDELSE
           WHERE KEY = ?
        """.trimIndent()) {
            setParams { setString(1, hendelsesId) }
            setRowMapper { row ->
                JoarkHendelse(
                    row.getString("key"),
                    row.getString("value")
                )
            }
        }
    }

}
