package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnVurderingRepository
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

//TODO: Må se på om faktagrunnlag.barn skal deles i to, en for registeropplysninger og en for delvurdering fra saksbehandler
class BarnetilleggService(
    private val barnVurderingRepository: BarnVurderingRepository,
    private val barnetilleggRepository: BarnetilleggRepository,
    private val sakOgBehandlingService: SakOgBehandlingService
) {
    fun beregn(behandlingId: BehandlingId): Tidslinje<RettTilBarnetillegg> {
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        var resultat: Tidslinje<RettTilBarnetillegg> =
            Tidslinje(listOf(Segment(sak.rettighetsperiode, RettTilBarnetillegg())))


        //hent saksbehanler else request saksbehandler
        val relevanteBarn = barnVurderingRepository.hentHvisEksisterer(behandlingId)?.Tidslinjer() ?: Tidslinje()


        resultat = resultat.kombiner(
            relevanteBarn,
            JoinStyle.CROSS_JOIN { periode, venstreSegment, høyreSegment ->
                val høyreVerdi = høyreSegment?.verdi
                val nyVenstreVerdi = venstreSegment?.verdi ?: RettTilBarnetillegg()
                if (høyreVerdi != null) {
                    nyVenstreVerdi.leggTilBarn(høyreVerdi)
                }

                Segment(periode, nyVenstreVerdi)
            })



        barnetilleggRepository.lagre(
            behandlingId,
            resultat.segmenter()
                .map {
                    BarnetilleggPeriode(
                        it.periode,
                        it.verdi.barn()
                    )
                }
        )

        return resultat.kryss(sak.rettighetsperiode)
    }
}