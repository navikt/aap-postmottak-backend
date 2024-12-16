package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.flate


import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.journalpostIdFraBehandlingResolver
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.authorizedGet

fun NormalOpenAPIRoute.struktureringApi(dataSource: HikariDataSource) {
    route("/api/behandling/{referanse}/grunnlag/strukturering") {
        authorizedGet<BehandlingsreferansePathParam, StruktureringGrunnlagDto>(
            AuthorizationParamPathConfig(
                journalpostPathParam = JournalpostPathParam(
                    "referanse",
                    journalpostIdFraBehandlingResolver(dataSource)
                )
            )
        ) { req ->
            val (kategorivurdering, struktureringsvurdering) = dataSource.transaction(readOnly = true) {
                val behandling = BehandlingRepositoryImpl(it).hent(req)
                val kategorivurdering = KategorivurderingRepository(it).hentKategoriAvklaring(behandling.id)
                val struktureringsvurdering =
                    StruktureringsvurderingRepository(it).hentStruktureringsavklaring(behandling.id)
                Pair(kategorivurdering, struktureringsvurdering)
            }

            checkNotNull(kategorivurdering) { "Behandlingen mangler kategorisering" }

            respond(
                StruktureringGrunnlagDto(
                    struktureringsvurdering
                        ?.vurdering?.let(::StruktureringVurderingDto),
                    kategorivurdering.avklaring,
                    listOf(1, 2)
                )
            )
        }
    }

}
