package no.nav.aap.fordeler

import no.nav.aap.fordeler.regler.ErIkkeAnkeRegel
import no.nav.aap.fordeler.regler.ErIkkeReisestønadRegel
import no.nav.aap.fordeler.regler.KelvinSakRegel
import no.nav.aap.fordeler.regler.Regel
import no.nav.aap.fordeler.regler.RegelFactory
import no.nav.aap.fordeler.regler.RegelInput
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(FordelerRegelService::class.java)

typealias RegelMap = Map<String, Boolean>

/**
 * @param systemNavn Systemet regelen ble evaluert til å tilhøre da regelen ble kjørt.
 */
data class Regelresultat(val regelMap: RegelMap, val forJournalpost: Long, val systemNavn: String? = null) {

    fun skalTilKelvin(): Boolean {
        val sakFinnesIKelvin =
            requireNotNull(regelMap[KelvinSakRegel::class.simpleName]) { "Mangler resultat fra ${KelvinSakRegel::class.simpleName}. JournalpostId: $forJournalpost" }
        val erIkkeReisestønad =
            requireNotNull(regelMap[ErIkkeReisestønadRegel::class.simpleName]) { "Mangler resultat fra ${ErIkkeReisestønadRegel::class.simpleName}. JournalpostId: $forJournalpost" }
        val erIkkeAnke =
            requireNotNull(regelMap[ErIkkeAnkeRegel::class.simpleName]) { "Mangler resultat fra ${ErIkkeAnkeRegel::class.simpleName}. JournalpostId: $forJournalpost" }

        if (sakFinnesIKelvin && erIkkeReisestønad && erIkkeAnke) {
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

    fun gikkTilKelvin(): Boolean {
        return systemNavn == "KELVIN"
    }
}

class FordelerRegelService(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider
) {
    fun evaluer(input: RegelInput): Regelresultat {
        return hentAktiveRegler(repositoryProvider, gatewayProvider)
            .associate { regel ->
                regel.regelNavn() to regel.vurder(input)
            }.let { Regelresultat(it, input.journalpostId) }
    }

    private fun hentAktiveRegler(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider
    ): List<Regel<RegelInput>> =
        RegelFactory::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .filter { it.erAktiv }
            .map { it.medDataInnhenting(repositoryProvider = repositoryProvider, gatewayProvider = gatewayProvider) }

}
