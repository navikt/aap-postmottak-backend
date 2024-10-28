package no.nav.aap.postmottak.fordeler

import org.slf4j.LoggerFactory
import no.nav.aap.postmottak.fordeler.regler.Aldersregel
import no.nav.aap.postmottak.fordeler.regler.ArenaSakRegel
import no.nav.aap.postmottak.fordeler.regler.RegelInput
private val log = LoggerFactory.getLogger(FordelerRegelService::class.java)


class FordelerRegelService {
    fun skalTilKelvin(input: RegelInput): Boolean {
        return listOf(
            Aldersregel.medDataInnhenting(),
            ArenaSakRegel.medDataInnhenting(),
        ).all {
            it.vurder(input).also { passed ->
                if (!passed) {
                    log.info("Regel ${it::class.simpleName} feilet for journalpost ${input.journalpostId}")
                }
            }
        }
    }
}