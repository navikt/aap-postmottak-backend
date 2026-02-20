package no.nav.aap.postmottak.api.test

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.fordeler.EnhetMedOppfølgingsKontor
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.fordeler.NorgGateway
import no.nav.aap.fordeler.VeilarbarenaGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.postmottak.gateway.EgenAnsattGateway
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

data class BehandlingsListe(
    val id: String,
    val journalpostId: String,
    val steg: String,
    val status: String,
    val opprettet: LocalDateTime
)

data class FinnEntitetRequest(
    @param:PathParam("ident")
    val ident: String
)

private val log = LoggerFactory.getLogger("no.nav.aap.postmottak.backend.test")

fun NormalOpenAPIRoute.testApi(
    dataSource: DataSource,
    gatewayProvider: GatewayProvider
) {
    val miljø = Miljø.er()
    if (miljø == MiljøKode.DEV || miljø == MiljøKode.LOKALT) {
        route("/test/hentAlleBehandlinger") {
            @Suppress("UnauthorizedGet")
            get<Unit, List<BehandlingsListe>> {
                val response = dataSource.transaction {
                    it.queryList(
                        """SELECT referanse as ref, steg, behandling.journalpost_id, behandling.OPPRETTET_TID, behandling.status as status FROM BEHANDLING
                            LEFT JOIN STEG_HISTORIKK ON STEG_HISTORIKK.BEHANDLING_ID = BEHANDLING.ID AND aktiv = true
                            ORDER BY opprettet_tid DESC
                            LIMIT 150
                        """.trimMargin()
                    ) {
                        setRowMapper {
                            BehandlingsListe(
                                it.getString("ref"),
                                it.getString("journalpost_id"),
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
        route("/test/finnEnhetMedOppfolgingskontor/{ident}") {
            get<FinnEntitetRequest, EnhetMedOppfølgingsKontor> { req ->
                val ident = Ident(req.ident)

                log.info("Finner enhet for : $req")

                val enhetsutreder = Enhetsutreder(
                    gatewayProvider.provide<NorgGateway>(),
                    gatewayProvider.provide<PersondataGateway>(),
                    gatewayProvider.provide<EgenAnsattGateway>(),
                    gatewayProvider.provide<VeilarbarenaGateway>(),
                )

                val response = enhetsutreder.finnEnhetMedOppfølgingskontor(Person(1, UUID.randomUUID(), listOf(ident)))
                respond(response)
            }
        }
    }
}