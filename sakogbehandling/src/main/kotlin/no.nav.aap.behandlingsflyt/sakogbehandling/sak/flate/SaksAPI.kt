package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import no.nav.aap.verdityper.sakogbehandling.Ident
import javax.sql.DataSource

fun NormalOpenAPIRoute.saksApi(dataSource: DataSource) {
    route("/api/sak") {
        route("/finn").post<Unit, List<SaksinfoDTO>, FinnSakForIdentDTO> { _, dto ->
            var saker: List<SaksinfoDTO> = emptyList()
            dataSource.transaction { connection ->
                val ident = Ident(dto.ident)
                val person = PersonRepository(connection).finn(ident)

                if (person == null) {
                    throw ElementNotFoundException()
                } else {
                    saker = SakRepositoryImpl(connection).finnSakerFor(person)
                        .map { sak ->
                            SaksinfoDTO(
                                saksnummer = sak.saksnummer.toString(),
                                periode = sak.rettighetsperiode
                            )
                        }

                }
            }
            respond(saker)
        }
        route("") {
            route("/alle").get<Unit, List<SaksinfoDTO>> {
                var saker: List<SaksinfoDTO> = emptyList()
                dataSource.transaction { connection ->
                    saker = SakRepositoryImpl(connection).finnAlle()
                        .map { sak ->
                            SaksinfoDTO(
                                saksnummer = sak.saksnummer.toString(),
                                periode = sak.rettighetsperiode
                            )
                        }
                }

                respond(saker)
            }
            route("/{saksnummer}").get<HentSakDTO, UtvidetSaksinfoDTO> { req ->
                val saksnummer = req.saksnummer
                var sak: Sak? = null
                var behandlinger: List<BehandlinginfoDTO> = emptyList()

                dataSource.transaction { connection ->
                    sak = SakRepositoryImpl(connection).hent(saksnummer = Saksnummer(saksnummer))

                    behandlinger = BehandlingRepositoryImpl(connection).hentAlleFor(sak!!.id).map { behandling ->
                        BehandlinginfoDTO(
                            referanse = behandling.referanse,
                            type = behandling.typeBehandling().identifikator(),
                            status = behandling.status(),
                            opprettet = behandling.opprettetTidspunkt
                        )
                    }
                }

                respond(
                    UtvidetSaksinfoDTO(
                        saksnummer = sak!!.saksnummer.toString(),
                        periode = sak!!.rettighetsperiode,
                        ident = sak!!.person.identer().first().identifikator,
                        behandlinger = behandlinger,
                        status = sak!!.status()
                    )
                )
            }
        }
    }
}
