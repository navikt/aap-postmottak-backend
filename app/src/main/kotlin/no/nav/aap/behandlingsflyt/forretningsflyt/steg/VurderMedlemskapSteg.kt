package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.StegType

class VurderMedlemskapSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekst): StegResultat {

        val behandlingId = kontekst.behandlingId
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.MEDLEMSKAP)
        val periodeTilVurdering =
            periodeTilVurderingService.utled(kontekst = kontekst, vilkår = Vilkårtype.SYKDOMSVILKÅRET)
        for (periode in periodeTilVurdering) {
            vilkår.leggTilVurdering(
                Vilkårsperiode(
                    periode = periode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
                    faktagrunnlag = null
                )
            )
        }
        vilkårsresultatRepository.lagre(behandlingId, vilkårsresultat)

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return VurderMedlemskapSteg(
                VilkårsresultatRepository(connection),
                PeriodeTilVurderingService(SakService(connection))
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_MEDLEMSKAP
        }
    }
}
