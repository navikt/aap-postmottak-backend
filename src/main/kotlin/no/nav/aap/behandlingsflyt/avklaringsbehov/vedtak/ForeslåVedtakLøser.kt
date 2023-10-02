package no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.kontroll.FlytKontekst

class ForeslåVedtakLøser : AvklaringsbehovsLøser<ForeslåVedtakLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: ForeslåVedtakLøsning): LøsningsResultat {
        // DO NOTHING 4 Now
        return LøsningsResultat(løsning.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FORESLÅ_VEDTAK
    }
}
