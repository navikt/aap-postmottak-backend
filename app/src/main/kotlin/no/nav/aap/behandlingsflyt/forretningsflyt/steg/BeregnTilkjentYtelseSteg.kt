package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.MinsteÅrligYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

private const val ANTALL_ÅRLIGE_ARBEIDSDAGER = 260

class BeregnTilkjentYtelseSteg private constructor(
    private val underveisRepository: UnderveisRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(BeregnTilkjentYtelseSteg::class.java)

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val beregningsgrunnlag = requireNotNull(beregningsgrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId))
        val underveisGrunnlag = underveisRepository.hent(kontekst.behandlingId)

        val underveisTidslinje = Tidslinje(underveisGrunnlag.perioder.map { Segment(it.periode, it) })

        val grunnlagsfaktor = beregningsgrunnlag.grunnlaget()

        val utgangspunktForÅrligYtelse = grunnlagsfaktor.multiplisert(Prosent.`66_PROSENT`)
        val minsteÅrligYtelseTidslinje = MinsteÅrligYtelse.tilTidslinje()
        val årligYtelseTidslinje = minsteÅrligYtelseTidslinje.mapValue{ minsteÅrligYtelse ->
            maxOf(requireNotNull(minsteÅrligYtelse), utgangspunktForÅrligYtelse)
        }

        val gradertÅrligYtelseTidslinje = underveisTidslinje.kombiner(
            årligYtelseTidslinje,
            JoinStyle.INNER_JOIN
        ) { periode, venstre, høyre ->
            val dagsats = høyre?.verdi?.dividert(ANTALL_ÅRLIGE_ARBEIDSDAGER) ?: GUnit(0)
            val utbetalingsgrad = venstre?.verdi?.utbetalingsgrad() ?: Prosent.`0_PROSENT`
            Segment(periode, TilkjentGUnit(dagsats, utbetalingsgrad))
        }


        val maksDagsatsHeleUttaket =
            gradertÅrligYtelseTidslinje.kombiner(
                Grunnbeløp.tilTidslinje(),
                JoinStyle.INNER_JOIN
            ) { periode, venstre, høyre ->
                val dagsats =
                    høyre?.verdi?.multiplisert(requireNotNull(venstre?.verdi?.dagsats))?: Beløp(0)

                val utbetalingsgrad = venstre?.verdi?.gradering ?: Prosent.`0_PROSENT`
                Segment(periode, Tilkjent(dagsats, utbetalingsgrad))
            }

        log.info("Beregnet tilkjent ytelse: $maksDagsatsHeleUttaket")

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BeregnTilkjentYtelseSteg(
                UnderveisRepository(connection),
                BeregningsgrunnlagRepository(connection)
            )
        }

        override fun type(): StegType {
            return StegType.BEREGN_TILKJENT_YTELSE
        }
    }
}

class Tilkjent(val dagsats: Beløp, val gradering: Prosent) {

    fun redusertDagsats(): Beløp {
        return dagsats.multiplisert(gradering)
    }

    override fun toString(): String {
        return "Tilkjent(dagsats=$dagsats, gradering=$gradering, redusertDagsats=${redusertDagsats()})"
    }
}

class TilkjentGUnit(val dagsats: GUnit, val gradering: Prosent) {

    fun redusertDagsats(): GUnit {
        return dagsats.multiplisert(gradering)
    }

    override fun toString(): String {
        return "Tilkjent(dagsats=$dagsats, gradering=$gradering, redusertDagsats=${redusertDagsats()})"
    }
}
