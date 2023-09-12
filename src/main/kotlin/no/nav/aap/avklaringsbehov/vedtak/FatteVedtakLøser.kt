package no.nav.aap.avklaringsbehov.vedtak

import no.nav.aap.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.avklaringsbehov.LøsningsResultat
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.kontroll.FlytKontekst

class FatteVedtakLøser : AvklaringsbehovsLøser<FatteVedtakLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: FatteVedtakLøsning): LøsningsResultat {
        // DO NOTHING 4 Now
        return LøsningsResultat(løsning.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FATTE_VEDTAK
    }
}
