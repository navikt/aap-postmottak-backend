package no.nav.aap.postmottak.api.faktagrunnlag.under18

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbStatus
import no.nav.aap.postmottak.api.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.finnPåKelvinSak
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.JournalpostTilgangRequest
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.TilgangGateway
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.under18Api(
    dataSource: DataSource, repositoryRegistry: RepositoryRegistry, gatewayProvider: GatewayProvider
) {
    route("/api/behandling/{referanse}/grunnlag/under18") {
        authorizedGet<BehandlingsreferansePathParam, Under18Grunnlag>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam("referanse", journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource))
            )
        ) { req ->
            val grunnlag = dataSource.transaction(readOnly = true) {
                repositoryRegistry.provider(it).hentUnder18Grunnlag(req)
            }
            val brukerToken = token()
            val svar = grunnlag.medDokumentbehandlinger(
                saf = gatewayProvider.provide(),
                kanLese = { id ->
                    TilgangGateway.harTilgangTilJournalpost(
                        JournalpostTilgangRequest(id.referanse, operasjon = Operasjon.SE, påkrevdRolle = emptyList()), brukerToken
                    ).tilgang
                },
                hentDokumentbehandling = { nyId ->
                    dataSource.transaction(readOnly = true) { connection ->
                        val repositories = repositoryRegistry.provider(connection)
                        val behandlinger = repositories.provide<BehandlingRepository>().hentAlleBehandlingerForJournalpost(nyId)
                            .filter { it.typeBehandling == TypeBehandling.DokumentHåndtering }
                        check(behandlinger.size <= 1) { "Flere dokumentbehandlinger for tilknyttet journalpost $nyId" }
                        behandlinger.singleOrNull()?.let { repositories.behandlingDto(it) }
                    }
                }
            )
            if (svar == null) respondWithStatus(HttpStatusCode.Forbidden) else respond(svar)
        }
    }
}

private fun RepositoryProvider.hentUnder18Grunnlag(req: BehandlingsreferansePathParam): Under18Grunnlag {
    val behandling = provide<BehandlingRepository>().hent(req)
    if (behandling.typeBehandling != TypeBehandling.Under18) {
        throw UgyldigForespørselException("Behandlingen er ikke en Under18-behandling")
    }
    val id = behandling.id
    val under18 = provide<Under18Repository>()
    return Under18Grunnlag(
        behandlingDto(behandling),
        provide<SaksnummerRepository>().hentSakVurdering(id)?.saksnummer,
        under18.hentAldersgrunnlag(id),
        under18.hentSøknadsavklaring(id),
        under18.hentAktivSamtykkevurdering(id),
        under18.hentSøknadsgrunnlag(id).søknader.sortedBy { it.referanse }.map { Under18SøknadDto(it, null, null) },
    )
}

private fun RepositoryProvider.behandlingDto(behandling: Behandling): Under18BehandlingDto {
    val behov = provide<AvklaringsbehovRepository>().hentAvklaringsbehovene(behandling.id).åpne()
    val feilet = provide<FlytJobbRepository>().hentJobberForBehandling(behandling.id.id)
        .any { it.type() == ProsesserBehandlingJobbUtfører.type && it.status() == JobbStatus.FEILET }
    return Under18BehandlingDto(
        behandling.referanse.referanse, behandling.versjon, behandling.status(), behandling.aktivtSteg(),
        behov.filterNot { it.erVentepunkt() }.map { it.definisjon },
        behov.filter { it.erVentepunkt() }.map { Under18VentDto(it.definisjon, it.historikk.last().frist) },
        feilet,
    )
}

private suspend fun Under18Grunnlag.medDokumentbehandlinger(
    saf: JournalpostGateway,
    kanLese: suspend (JournalpostId) -> Boolean,
    hentDokumentbehandling: (JournalpostId) -> Under18BehandlingDto?,
): Under18Grunnlag? {
    val journalposter = søknader.map { it.journalpostId } +
        samtykkevurdering?.samtykkeJournalpostIder.orEmpty()
    if (!journalposter.distinct().all { kanLese(it) }) return null
    val sak = saksnummer ?: return this
    return copy(søknader = søknader.map { søknad ->
        val kilde = saf.hentJournalpost(søknad.journalpostId)
        val tilknyttede = saf.hentTilknyttedeJournalposter(DokumentInfoId(kilde.dokumenter.first().dokumentInfoId))
        val nyId = tilknyttede.finnPåKelvinSak(søknad.journalpostId, kilde.dokumenter.map { it.dokumentInfoId }.toSet(), sak)
            ?: return@map søknad
        if (!kanLese(nyId)) return null
        søknad.copy(tilknyttetJournalpostId = nyId, dokumentbehandling = hentDokumentbehandling(nyId))
    })
}
