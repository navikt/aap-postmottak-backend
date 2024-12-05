package no.nav.aap.postmottak.fordeler

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.postmottak.fordeler.regler.Regel
import org.slf4j.LoggerFactory
import no.nav.aap.postmottak.fordeler.regler.RegelFactory
import no.nav.aap.postmottak.fordeler.regler.RegelInput

private val log = LoggerFactory.getLogger(FordelerRegelService::class.java)

typealias RegelMap = Map<String, Boolean>

data class Regelresultat(val regelMap: RegelMap) {
    fun skalTilKelvin() = regelMap.values.all { it }
}

class FordelerRegelService(private val prometheus: MeterRegistry = SimpleMeterRegistry()) {
    fun evaluer(input: RegelInput): Regelresultat {
        return hentAktiveRegler()
            .associate { regel ->
                regel.regelNavn() to regel.vurder(input)
                    .also { if (!it) log.info("Validering av regel ${regel.regelNavn()} ga false: journalpost ${input.journalpostId} skal ikke til Kelvin") }
            }.let(::Regelresultat)
    }

    private fun hentAktiveRegler(): List<Regel<RegelInput>> = RegelFactory::class.sealedSubclasses
        .mapNotNull { it.objectInstance }
        .filter { it.erAktiv }
        .map { it.medDataInnhenting() }

}
