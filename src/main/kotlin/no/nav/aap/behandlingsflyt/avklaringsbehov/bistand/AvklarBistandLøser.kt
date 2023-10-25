package no.nav.aap.behandlingsflyt.avklaringsbehov.bistand

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandsRepository

class AvklarBistandLøser : AvklaringsbehovsLøser<AvklarBistandsbehovLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarBistandsbehovLøsning): LøsningsResultat {
        val behandling = BehandlingRepository.hent(kontekst.behandlingId)

        BistandsRepository.lagre(
            behandlingId = behandling.id,
            bistandsVurdering = løsning.bistandsVurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.bistandsVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
