package no.nav.aap.behandlingsflyt.beregning.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag

import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent
import java.math.BigDecimal
import java.time.Year

class BeregningDTO(
    val grunnlag: GUnit,
    val faktagrunnlag: Faktagrunnlag,
    val grunnlag11_19: Grunnlag11_19DTO,
    val grunnlagUføre: GrunnlagUføreDTO?=null,
    val grunnlagYrkesskade: GrunnlagYrkesskadeDTO?=null,
)

class GrunnlagUføreDTO(
    private val grunnlaget: BigDecimal,
    private val type: String,
    private val grunnlag: Grunnlag11_19DTO,
    private val grunnlagYtterligereNedsatt: Grunnlag11_19DTO,
    private val uføregrad: Int,
    private val uføreInntekterFraForegåendeÅr: List<Pair<Int,BigDecimal>>, //uføre ikke oppjustert
    private val uføreInntektIKroner: BigDecimal, //grunnlaget
    private val uføreYtterligereNedsattArbeidsevneÅr: Int,
    private val er6GBegrenset: Boolean, //skal være individuelt på hver inntekt
    private val erGjennomsnitt: Boolean,
)

class Grunnlag11_19DTO(
    private val grunnlaget: BigDecimal,
    private val er6GBegrenset: Boolean,
    private val erGjennomsnitt: Boolean,
    private val inntekter: List<Pair<Int,BigDecimal>>,
)

class GrunnlagYrkesskadeDTO(
    private val grunnlaget: BigDecimal,
    private val beregningsgrunnlag: Grunnlag11_19DTO,
    private val terskelverdiForYrkesskade: Int,
    private val andelSomSkyldesYrkesskade: BigDecimal,
    private val andelYrkesskade: Int,
    private val benyttetAndelForYrkesskade: Int,
    private val andelSomIkkeSkyldesYrkesskade: BigDecimal,
    private val antattÅrligInntektYrkesskadeTidspunktet: BigDecimal,
    private val yrkesskadeTidspunkt: Int,
    private val grunnlagForBeregningAvYrkesskadeandel: BigDecimal,
    private val yrkesskadeinntektIG: BigDecimal,
    private val grunnlagEtterYrkesskadeFordel: BigDecimal,
    private val er6GBegrenset: Boolean,
    private val erGjennomsnitt: Boolean
)

