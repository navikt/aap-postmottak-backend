package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet
import java.util.UUID

fun NormalOpenAPIRoute.finnSakApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/finnSak") {
        authorizedGet<Behandlingsreferanse, AvklarSakGrunnlagDto>(JournalpostPathParam("referanse")) { req ->
            val response = dataSource.transaction(readOnly = true) {
                val behandling = BehandlingRepositoryImpl(it).hent(req)
                val saksvurdering = SaksnummerRepository(it).hentSakVurdering(behandling.id)
                val relaterteSaker = SaksnummerRepository(it).hentSaksnummre(behandling.id)

                AvklarSakGrunnlagDto(
                    vurdering = saksvurdering?.let { AvklarSakVurderingDto.toDto(saksvurdering) },
                    saksinfo = relaterteSaker.map { SaksInfoDto(it.saksnummer, it.periode) }
                )
            }
            respond(response)
        }
    }

}
