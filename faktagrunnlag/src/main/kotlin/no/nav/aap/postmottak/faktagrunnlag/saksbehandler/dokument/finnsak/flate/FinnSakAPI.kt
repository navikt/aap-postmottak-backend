package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.DokumentbehandlingRepository

fun NormalOpenAPIRoute.finnSakApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/finnSak") {
        get<JournalpostId, AvklarSakGrunnlagDto> { req ->
            val response = dataSource.transaction(readOnly = true) {
                val behandling = DokumentbehandlingRepository(it).hent(req, null)
                val saksvurdering = behandling.vurderinger.saksvurdering
                val relaterteSaker = SaksnummerRepository(it).hentSaksnummre(behandling.id)

                AvklarSakGrunnlagDto(
                    vurdering = saksvurdering?.let { AvklarSakVurderingDto(saksvurdering.toString()) },
                    saksinfo = relaterteSaker.map { SaksInfoDto(it.saksnummer, it.periode) }
                )
            }
            respond(response)
        }
    }

}
