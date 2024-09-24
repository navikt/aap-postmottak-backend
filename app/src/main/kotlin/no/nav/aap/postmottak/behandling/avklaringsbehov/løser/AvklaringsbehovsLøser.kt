package no.nav.aap.postmottak.behandling.avklaringsbehov.løser

import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

sealed interface AvklaringsbehovsLøser<in T : AvklaringsbehovLøsning> {

    fun løs(kontekst: AvklaringsbehovKontekst, løsning: T): LøsningsResultat

    fun forBehov(): Definisjon
}
