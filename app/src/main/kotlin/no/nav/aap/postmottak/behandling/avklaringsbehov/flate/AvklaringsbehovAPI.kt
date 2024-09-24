package no.nav.aap.postmottak.behandling.avklaringsbehov.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.postmottak.behandling.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.postmottak.behandling.avklaringsbehov.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.bruker
import org.slf4j.MDC
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklaringsbehovApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/løs-behov") {
            post<Unit, LøsAvklaringsbehovPåBehandling, LøsAvklaringsbehovPåBehandling> { _, request ->
                dataSource.transaction { connection ->
                        val behandling = BehandlingRepositoryImpl(connection).hent(request.referanse)
                        MDC.putCloseable("behandlingId", behandling.id.toString()).use {
                            BehandlingTilstandValidator(connection).validerTilstand(
                                request.referanse,
                                request.behandlingVersjon
                            )

                            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                                key = behandling.id,
                                hendelse = LøsAvklaringsbehovBehandlingHendelse(
                                    request.behov,
                                    request.ingenEndringIGruppe ?: false,
                                    request.behandlingVersjon,
                                    bruker()
                                )
                            )
                        }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}