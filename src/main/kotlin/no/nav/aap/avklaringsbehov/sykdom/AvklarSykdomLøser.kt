package no.nav.aap.avklaringsbehov.sykdom

import no.nav.aap.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Vilkårsperiode
import no.nav.aap.domene.behandling.Vilkårstype
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.kontroll.FlytKontekst

class AvklarSykdomLøser : AvklaringsbehovsLøser<AvklarSykdomLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarSykdomLøsning) {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)

        val vilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårstype.SYKDOMSVILKÅRET)

        for (periodeMedUtfall in løsning.vurdertePerioder) {
            vilkåret.leggTilVurdering(
                vilkårsperiode = Vilkårsperiode(
                    periode = periodeMedUtfall.periode,
                    utfall = periodeMedUtfall.utfall,
                    manuellVurdering = true,
                    faktagrunnlag = SykdomsFaktagrunnlag()
                )
            )
        }

    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}
