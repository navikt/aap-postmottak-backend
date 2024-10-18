package no.nav.aap.postmottak.test

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import java.time.LocalDateTime
import javax.sql.DataSource

data class BehandlingsListe(
    val id: String,
    val steg: String,
    val status: String,
    val opprettet: LocalDateTime
)


fun NormalOpenAPIRoute.testApi(datasource: DataSource) {
    val miljø = Miljø.er()
    if (miljø == MiljøKode.DEV || miljø == MiljøKode.LOKALT) {
        route("/test/hentAlleBehandlinger") {
            @Suppress("UnauthorizedGet")
            get<Unit, List<BehandlingsListe>> {
                val response = datasource.transaction {
                    it.queryList(
                        """SELECT journalpost_id as ref, steg, behandling.OPPRETTET_TID, behandling.status as status FROM BEHANDLING
                            LEFT JOIN STEG_HISTORIKK ON STEG_HISTORIKK.BEHANDLING_ID = BEHANDLING.ID AND aktiv = true
                        """.trimMargin()
                    ) {
                        setRowMapper {
                            BehandlingsListe(
                                it.getString("ref"),
                                it.getString("steg"),
                                it.getString("status"),
                                it.getLocalDateTime("OPPRETTET_TID")
                            )
                        }
                    }
                }
                respond(response)
            }
        }
    }
}