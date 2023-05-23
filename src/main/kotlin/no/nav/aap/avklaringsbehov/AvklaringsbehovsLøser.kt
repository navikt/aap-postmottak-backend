package no.nav.aap.avklaringsbehov

import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.kontroll.FlytKontekst

interface AvklaringsbehovsLøser<T : AvklaringsbehovLøsning> {

    fun løs(kontekst: FlytKontekst, løsning: T)

    fun forBehov(): Definisjon
}
