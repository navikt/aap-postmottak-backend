package no.nav.aap.avklaringsbehov.sykdom

import no.nav.aap.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.avklaringsbehov.LøsningsResultat
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.domene.behandling.grunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.flyt.kontroll.FlytKontekst

class AvklarYrkesskadeLøser : AvklaringsbehovsLøser<AvklarYrkesskadeLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarYrkesskadeLøsning): LøsningsResultat {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)
        val sykdomsGrunnlag = SykdomsTjeneste.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

        SykdomsTjeneste.lagre(
            behandlingId = behandling.id,
            yrkesskadevurdering = løsning.yrkesskadevurdering,
            sykdomsvurdering = sykdomsGrunnlag?.sykdomsvurdering
        )

        return LøsningsResultat(begrunnelse = løsning.yrkesskadevurdering.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}
