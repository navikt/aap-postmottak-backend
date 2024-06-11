package no.nav.aap.behandlingsflyt.vilkår.alder.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.aldersGrunnlagApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/alder") {
            get<BehandlingReferanse, AlderDTO> { req ->

                val alderDTO = AlderDTO(LocalDate.now());


                respond(alderDTO)
            }
        }
    }
}