package no.nav.aap.postmottak.fordeler

import no.nav.aap.postmottak.fordeler.regler.Aldersregel
import no.nav.aap.postmottak.fordeler.regler.RegelInput

class FordelerRegelService {
    suspend fun skalTilKelvin(input: RegelInput): Boolean {
        return listOf(
            Aldersregel.medDataInnhenting()
        ).all { it.vurder(input) }
    }
}