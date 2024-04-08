package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService


fun NormalOpenAPIRoute.sykepengerGrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykepengergrunnlag") {
            get<BehandlingReferanse, SykepengerVurdering> { req ->
                val sykepengerErstatningGrunnlag = dataSource.transaction { connection ->
                    val behandling: Behandling = BehandlingReferanseService(connection).behandling(req)
                    SykepengerErstatningRepository(connection).hentHvisEksisterer(behandlingId = behandling.id)
                }

                //TODO: feilhåndetering
                if (sykepengerErstatningGrunnlag?.vurdering != null) {
                    respond(
                        sykepengerErstatningGrunnlag.vurdering
                    )
                }else {
                    //antar false om ikke funnet
                    respond(
                        SykepengerVurdering(
                            begrunnelse = "Fant ikke sykepengerErstatningGrunnlag",
                            harRettPå = false,
                            dokumenterBruktIVurdering = emptyList()
                        )
                    )
                }
            }
        }
    }
}