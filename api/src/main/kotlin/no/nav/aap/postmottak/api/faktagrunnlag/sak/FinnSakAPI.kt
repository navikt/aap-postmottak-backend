package no.nav.aap.postmottak.api.faktagrunnlag.sak

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.api.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.faktagrunnlag.register.PersonRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.PersonIdentPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.finnSakApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/{referanse}/grunnlag/finnSak") {
        authorizedGet<BehandlingsreferansePathParam, AvklarSakGrunnlagDto>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse",
                    journalpostIdFraBehandlingResolver(repositoryRegistry, dataSource)
                )
            )
        ) { req ->
            val response = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val saksnummerRepository = repositoryProvider.provide(SaksnummerRepository::class)
                val behandling = repositoryProvider.provide(BehandlingRepository::class).hent(req)

                val saksvurdering = saksnummerRepository.hentSakVurdering(behandling.id)
                val relaterteSaker = saksnummerRepository.hentKelvinSaker(behandling.id)

                val journalpost =
                    requireNotNull(
                        repositoryProvider.provide(JournalpostRepository::class).hentHvisEksisterer(req)
                    ) { "Journalpost ikke funnet. Behandling: ${req.referanse}" }

                AvklarSakGrunnlagDto(
                    vurdering = saksvurdering?.let { AvklarSakVurderingDto.toDto(saksvurdering) },
                    saksinfo = relaterteSaker.map { SaksInfoDto(it.saksnummer, it.periode) },
                    brevkode = journalpost.hoveddokumentbrevkode,
                    journalposttittel = journalpost.tittel,
                    dokumenter = journalpost.dokumenter,
                    kanEndreAvsenderMottaker = !journalpost.kanal.erDigitalKanal(),
                    avsenderMottaker = journalpost.avsenderMottaker,
                )
            }
            respond(response)
        }
    }

    route("/api/behandling/{ident}/behandlinger") {
        authorizedGet<String, FinnBehandlingerResponse>(
            AuthorizationParamPathConfig(
                personIdentPathParam = PersonIdentPathParam("ident"),
            )
        ) { req ->
            val response = dataSource.transaction(readOnly = true) { it ->
                val behandlingRepository = repositoryRegistry.provider(it).provide<BehandlingRepository>()
                val personRepository = repositoryRegistry.provider(it).provide<PersonRepository>()
                val person = personRepository.finn(Ident(req)) ?: return@transaction FinnBehandlingerResponse(
                    behandlinger = emptyList()
                )

                FinnBehandlingerResponse(
                    behandlingRepository.hentBehandlingerForPerson(person)
                        .map {
                            BehandlinginfoDTO(
                                referanse = it.referanse.referanse,
                                journalPostId = it.journalpostId.referanse.toString(),
                                typeBehandling = it.typeBehandling,
                                status = it.status(),
                                opprettet = it.opprettetTidspunkt
                            )
                        })
            }

            respond(response)
        }
    }

}
