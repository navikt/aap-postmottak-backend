package no.nav.aap.postmottak.avklaringsbehov.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.postmottak.avklaringsbehov.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.postmottak.faktagrunnlag.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.postmottak.journalpostogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.MDC
import tilgang.Operasjon
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklaringsbehovApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/løs-behov") {
            authorizedPost<Unit, LøsAvklaringsbehovPåBehandling, LøsAvklaringsbehovPåBehandling>(
                AuthorizationBodyPathConfig(
                    Operasjon.SAKSBEHANDLE,
                    journalpostIdResolver = journalpostIdFraBehandlingResolver(dataSource)
                )
            ) { _, request ->
                dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val journalpostRepository = repositoryProvider.provide(JournalpostRepository::class)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    // TODO: Undersøk om vi trenger denne
                    val taSkriveLåsRepository = repositoryProvider.provide(TaSkriveLåsRepository::class)
                    val avklaringsbehovRepository = repositoryProvider.provide(AvklaringsbehovRepository::class)

                    val behandling = behandlingRepository.hent(request.referanse)

                    val lås = taSkriveLåsRepository.lås(request.referanse)
                    MDC.putCloseable("behandlingId", behandling.id.toString()).use {
                        val flytJobbRepository = FlytJobbRepository(connection)
                        BehandlingTilstandValidator(
                            BehandlingReferanseService(repositoryProvider.provide(BehandlingRepository::class)),
                            FlytJobbRepository(connection)
                        ).validerTilstand(
                            request.referanse,
                            request.behandlingVersjon
                        )

                        AvklaringsbehovHendelseHåndterer(
                            behandlingRepository,
                            avklaringsbehovRepository,
                            AvklaringsbehovOrkestrator(
                                connection,
                                BehandlingHendelseServiceImpl(
                                    flytJobbRepository,
                                    journalpostRepository
                                )
                            ),
                        ).håndtere(
                            key = behandling.id,
                            hendelse = LøsAvklaringsbehovBehandlingHendelse(
                                request.behov,
                                request.ingenEndringIGruppe ?: false,
                                request.behandlingVersjon,
                                bruker()
                            )
                        )
                        taSkriveLåsRepository.verifiserSkrivelås(lås)
                    }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}