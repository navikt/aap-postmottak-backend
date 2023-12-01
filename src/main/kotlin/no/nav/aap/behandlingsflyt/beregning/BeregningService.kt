package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.beregning.år.InntektsBehov
import no.nav.aap.behandlingsflyt.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomRepository

class BeregningService(
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository
) {

    fun beregnGrunnlag(behandlingId: BehandlingId): GUnit {
        val inntektGrunnlag = inntektGrunnlagRepository.hent(behandlingId)

        val inntekter = utledInput(behandlingId)

        val grunnlag11_19 =
            GrunnlagetForBeregningen(inntekter.utledForOrdinær(inntektGrunnlag.inntekter)).beregnGrunnlaget()
        return grunnlag11_19.grunnlaget()
    }

    private fun utledInput(behandlingId: BehandlingId): InntektsBehov {
        val sykdomGrunnlag = sykdomRepository.hent(behandlingId)

        return InntektsBehov(Input(sykdomGrunnlag.sykdomsvurdering?.nedsattArbeidsevneDato!!))
    }
}