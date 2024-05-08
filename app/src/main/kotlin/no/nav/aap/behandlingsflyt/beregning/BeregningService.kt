package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.Year

class BeregningService(
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val uføreRepository: UføreRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository
) {

    fun beregnGrunnlag(behandlingId: BehandlingId): Beregningsgrunnlag {
        val inntektGrunnlag = inntektGrunnlagRepository.hent(behandlingId)
        val sykdomGrunnlag = sykdomRepository.hent(behandlingId)
        val uføre = uføreRepository.hentHvisEksisterer(behandlingId)
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)

        val inntekter = utledInput(sykdomGrunnlag.sykdomsvurdering!!, beregningVurdering)

        val inntekterYtterligereNedsatt = inntekter.utledForYtterligereNedsatt(inntektGrunnlag.inntekter)
        val uføregrad = uføre?.vurdering?.uføregrad

        val beregningMedYrkesskade =
            if (inntekterYtterligereNedsatt != null && uføregrad != null) {
                val beregningMedYrkesskadeVedYtterligereNedsatt = beregn(sykdomGrunnlag, beregningVurdering, inntekterYtterligereNedsatt)
                val uføreberegning = UføreBeregning(
                    grunnlag = beregningMedYrkesskadeVedYtterligereNedsatt,
                    ytterligereNedsattGrunnlag = beregningMedYrkesskadeVedYtterligereNedsatt,
                    //TODO:
                    // Hva hvis bruker har flere uføregrader?
                    // Skal saksbahandler velge den som er knyttet til ytterligere nedsatt-tidspunktet?
                    uføregrad = uføregrad
                )
                uføreberegning.beregnUføre()
            }else {
                beregn(sykdomGrunnlag, beregningVurdering, inntekter.utledForOrdinær(inntektGrunnlag.inntekter))
            }


        beregningsgrunnlagRepository.lagre(behandlingId, beregningMedYrkesskade)

        return beregningMedYrkesskade
    }

    private fun utledInput(sykdomsvurdering: Sykdomsvurdering, vurdering: BeregningVurdering?): Inntektsbehov {
        return Inntektsbehov(
            Input(
                nedsettelsesDato = requireNotNull(sykdomsvurdering.nedsattArbeidsevneDato).atMonth(1).atDay(1),
                ytterligereNedsettelsesDato = vurdering?.ytterligereNedsattArbeidsevneDato
            )
        )
    }

    private fun beregn(
        sykdomGrunnlag: SykdomGrunnlag,
        beregningVurdering: BeregningVurdering?,
        inntekterPerÅr: Set<InntektPerÅr>
    ): Beregningsgrunnlag {
        val grunnlag11_19 =
            GrunnlagetForBeregningen(inntekterPerÅr).beregnGrunnlaget()

        val skadetidspunkt = sykdomGrunnlag.yrkesskadevurdering?.skadetidspunkt
        val antattÅrligInntekt = beregningVurdering?.antattÅrligInntekt
        val andelAvNedsettelsenSomSkyldesYrkesskaden = sykdomGrunnlag.yrkesskadevurdering?.andelAvNedsettelse
        if (skadetidspunkt != null && antattÅrligInntekt != null && andelAvNedsettelsenSomSkyldesYrkesskaden != null) {
            val inntektPerÅr = InntektPerÅr(
                Year.from(skadetidspunkt),
                antattÅrligInntekt
            )
            val yrkesskaden = YrkesskadeBeregning(
                grunnlag11_19 = grunnlag11_19,
                antattÅrligInntekt = inntektPerÅr,
                andelAvNedsettelsenSomSkyldesYrkesskaden = andelAvNedsettelsenSomSkyldesYrkesskaden
            ).beregnYrkesskaden()
            return yrkesskaden
        }

        return grunnlag11_19
    }
}
