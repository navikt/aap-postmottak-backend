package no.nav.aap.behandlingsflyt.avklaringsbehov.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService

fun NormalOpenAPIRoute.fatteVedtakGrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/fatte-vedtak") {
            get<BehandlingReferanse, FatteVedtakGrunnlagDto> { req ->

                val dto = dataSource.transaction { connection ->
                    val behandling: Behandling = BehandlingReferanseService(connection).behandling(req)
                    val avklaringsbehovene =
                        AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandling.id)

                    val vurderinger = totrinnsVurdering(avklaringsbehovene)
                    FatteVedtakGrunnlagDto(vurderinger = vurderinger, historikk = utledHistorikk(avklaringsbehovene))
                }
                respond(dto)
            }
        }
    }
}

fun utledHistorikk(avklaringsbehovene: Avklaringsbehovene): List<Historikk> {
    val relevanteBehov =
        avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FORESLÅ_VEDTAK, Definisjon.FATTE_VEDTAK))

    return relevanteBehov.map {
        it.historikk.filter { e -> e.status in listOf(Status.AVSLUTTET) }
            .map { at -> Historikk(it.definisjon, at.tidsstempel, at.endretAv) }
    }.flatten().sorted()
}

private fun totrinnsVurdering(avklaringsbehovene: Avklaringsbehovene): List<TotrinnsVurdering> {
    return avklaringsbehovene.alle()
        .filter { it.erTotrinn() }
        .map { tilTotrinnsVurdering(it) }
}

private fun tilTotrinnsVurdering(it: Avklaringsbehov): TotrinnsVurdering {
    return if (it.erTotrinnsVurdert()) {
        val sisteVurdering =
            it.historikk.lastOrNull { it.status in setOf(Status.SENDT_TILBAKE_FRA_BESLUTTER, Status.TOTRINNS_VURDERT) }
        val godkjent = it.status() == Status.TOTRINNS_VURDERT

        TotrinnsVurdering(it.definisjon.kode, godkjent, sisteVurdering?.begrunnelse)
    } else {
        TotrinnsVurdering(it.definisjon.kode, null, null)
    }
}