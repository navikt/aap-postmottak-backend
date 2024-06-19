package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.beregning.BeregningService
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

class FastsettGrunnlagSteg(
    private val beregningService: BeregningService,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(FastsettGrunnlagSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val beregningsgrunnlag = beregningService.beregnGrunnlag(kontekst.behandlingId)

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)

        val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.GRUNNLAGET)

        kontekst.perioderTilVurdering.forEach { periode ->
            vilkår.leggTilVurdering(
                Vilkårsperiode(
                    periode = periode,
                    utfall = Utfall.OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    innvilgelsesårsak = null,
                    avslagsårsak = null,
                    faktagrunnlag = beregningsgrunnlag.faktagrunnlag()
                )
            )
        }

        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

        log.info("Beregnet grunnlag til ${beregningsgrunnlag.grunnlaget()}")

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FastsettGrunnlagSteg(
                BeregningService(
                    InntektGrunnlagRepository(connection),
                    SykdomRepository(connection),
                    StudentRepository(connection),
                    UføreRepository(connection),
                    BeregningsgrunnlagRepository(connection),
                    BeregningVurderingRepository(connection)
                ),
                VilkårsresultatRepository(connection)
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_GRUNNLAG
        }
    }
}