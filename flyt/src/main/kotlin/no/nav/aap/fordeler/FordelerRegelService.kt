package no.nav.aap.fordeler

import no.nav.aap.fordeler.regler.ErIkkeReisestønadRegel
import no.nav.aap.fordeler.regler.KelvinSakRegel
import no.nav.aap.fordeler.regler.Regel
import no.nav.aap.fordeler.regler.RegelFactory
import no.nav.aap.fordeler.regler.RegelInput
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(FordelerRegelService::class.java)

typealias RegelMap = Map<String, Boolean>

data class Regelresultat(val regelMap: RegelMap) {
    fun skalTilKelvin(): Boolean {
        if (Miljø.er() == MiljøKode.PROD) {
            return false
        }
        
        val kelvinSakRegel = regelMap[KelvinSakRegel::class.simpleName] ?: false
        val erIkkeReisestønad = regelMap[ErIkkeReisestønadRegel::class.simpleName]!!
        if (kelvinSakRegel && erIkkeReisestønad) {
            log.info("Evaluering av KelvinSakRegel ga true: journalpost skal til Kelvin")
            return true
        }
        val reglerTilEvaluering = regelMap.filter { it.key != KelvinSakRegel::class.simpleName }
        return reglerTilEvaluering.values.all { it }.also {
            log.info(
                "Skal til Kelvin: $it. ${
                    if (!it) "\n Følgende regler ga false: ${
                        reglerTilEvaluering.filter { !it.value }.map { it.key }
                    }" else ""
                }"
            )
        }
    }
}

class FordelerRegelService(private val connection: DBConnection) {
    fun evaluer(input: RegelInput): Regelresultat {
        return hentAktiveRegler(connection)
            .associate { regel ->
                regel.regelNavn() to regel.vurder(input)
            }.let(::Regelresultat)
    }

    private fun hentAktiveRegler(connection: DBConnection): List<Regel<RegelInput>> = RegelFactory::class.sealedSubclasses
        .mapNotNull { it.objectInstance }
        .filter { it.erAktiv }
        .map { it.medDataInnhenting(connection) }

}
