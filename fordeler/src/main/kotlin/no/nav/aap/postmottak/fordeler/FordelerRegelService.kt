package no.nav.aap.postmottak.fordeler

import org.slf4j.LoggerFactory
import no.nav.aap.postmottak.fordeler.regler.RegelFactory
import no.nav.aap.postmottak.fordeler.regler.RegelInput

private val log = LoggerFactory.getLogger(FordelerRegelService::class.java)

typealias RegelMap = Map<String, Boolean>

data class Regelresultat(val regelMap: RegelMap) {
    fun skalTilKelvin() = regelMap.values.all { it }
}

class FordelerRegelService {
    fun evaluer(input: RegelInput): Regelresultat {
        return RegelFactory::class.sealedSubclasses
            .map { it.objectInstance }
            .filterNotNull()
            .filter { it.erAktiv }
            .map { it.medDataInnhenting() }
            .associate { regel ->
                regel::class.simpleName!! to regel.vurder(input)
                    .also { if (it) log.info("Validering av regel ${regel::class.simpleName} ga false: journalpost ${input.journalpostId} skal ikke til Kelvin") }
            }.let(::Regelresultat)
    }
}
