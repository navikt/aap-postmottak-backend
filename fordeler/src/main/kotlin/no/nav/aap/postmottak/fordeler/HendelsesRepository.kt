package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.DBConnection

data class JoarkHendelse(
    val hendelsesid: HendelsesId,
    val hendelse: String
)

class HendelsesRepository(private val connection: DBConnection) {

    fun hentHendelse(hendelsesId: HendelsesId): JoarkHendelse {
        return connection.queryFirst("""
           SELECT * FROM JOARK_HENDELSE
           WHERE HENDELSESID = ?
        """.trimIndent()) {
            setParams { setString(1, hendelsesId) }
            setRowMapper { row ->
                JoarkHendelse(
                    row.getString("HENDELSESID"),
                    row.getString("HENDELSE")
                )
            }
        }
    }

    fun lagreHendelse(joarkHendelse: JoarkHendelse) {
        connection.execute("""
            INSERT INTO JOARK_HENDELSE(HENDELSESID, HENDELSE) VALUES (?, ?)
        """.trimIndent()) {
            setParams {
                setString(1, joarkHendelse.hendelsesid)
                setString(2, joarkHendelse.hendelse)
            }
        }
    }

}
