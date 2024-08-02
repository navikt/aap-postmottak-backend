package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.auth.bruker
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.server.respondWithStatus
import org.slf4j.MDC
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklaringsbehovApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/løs-behov") {
            post<Unit, LøsAvklaringsbehovPåBehandling, LøsAvklaringsbehovPåBehandling> { _, request ->
                dataSource.transaction { connection ->
                    val behandlingRepository = BehandlingRepositoryImpl(connection)
                    val sakRepository = SakRepositoryImpl(connection)
                    behandlingRepository.hentMedLås(request.referanse)
                    val behandling = behandlingRepository.hent(request.referanse)
                    sakRepository.låsSak(behandling.sakId)
                    MDC.putCloseable("sakId", behandling.sakId.toString()).use {
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
                                    pipeline.context.bruker()
                                )
                            )
                            behandlingRepository.bumpVersjon(behandling.id)
                            sakRepository.bumpVersjon(behandling.sakId)
                        }
                    }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}