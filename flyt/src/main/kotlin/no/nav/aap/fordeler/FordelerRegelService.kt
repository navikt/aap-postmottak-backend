package no.nav.aap.fordeler

import no.nav.aap.fordeler.regler.ArenaSakRegel
import no.nav.aap.fordeler.regler.ErIkkeAnkeRegel
import no.nav.aap.fordeler.regler.ErIkkeReisestønadRegel
import no.nav.aap.fordeler.regler.KelvinSakRegel
import no.nav.aap.fordeler.regler.ManueltOverstyrtTilArenaRegel
import no.nav.aap.fordeler.regler.Regel
import no.nav.aap.fordeler.regler.RegelFactory
import no.nav.aap.fordeler.regler.RegelInput
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.regelresultat
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(FordelerRegelService::class.java)

typealias RegelMap = Map<String, Boolean>

/**
 * @param systemNavn Systemet regelen ble evaluert til å tilhøre da regelen ble kjørt.
 */
data class Regelresultat(
    val regelMap: RegelMap,
    val forJournalpost: Long,
    val systemNavn: String? = null
) {

    fun skalTilKelvin(): Boolean {
        val manueltOverstyrtTilArena = regelMap[ManueltOverstyrtTilArenaRegel::class.simpleName] ?: false
        if (manueltOverstyrtTilArena) {
            log.info("Sak med journalpostId=$forJournalpost går til Arena pga manuell overstyring")
            return false
        }

        val sakFinnesIKelvin =
            requireNotNull(regelMap[KelvinSakRegel::class.simpleName]) { "Mangler resultat fra ${KelvinSakRegel::class.simpleName}. JournalpostId: $forJournalpost" }
        val sakFinnesIArena =
            requireNotNull(regelMap[ArenaSakRegel::class.simpleName]) { "Mangler resultat fra ${ArenaSakRegel::class.simpleName}. JournalpostId: $forJournalpost" }
        val erIkkeReisestønad =
            requireNotNull(regelMap[ErIkkeReisestønadRegel::class.simpleName]) { "Mangler resultat fra ${ErIkkeReisestønadRegel::class.simpleName}. JournalpostId: $forJournalpost" }
        val erIkkeAnke =
            requireNotNull(regelMap[ErIkkeAnkeRegel::class.simpleName]) { "Mangler resultat fra ${ErIkkeAnkeRegel::class.simpleName}. JournalpostId: $forJournalpost" }

        if (sakFinnesIKelvin && erIkkeReisestønad && erIkkeAnke) {
            log.info("Evaluering av KelvinSakRegel ga true: journalpostId=$forJournalpost skal til Kelvin")
            return true
        }

        if (!sakFinnesIKelvin && sakFinnesIArena) {
            log.info("Personen har sak i Arena og ikke i Kelvin: journalpostId=$forJournalpost")
        }

        val reglerTilEvaluering = regelMap.filter {
            it.key != KelvinSakRegel::class.simpleName &&
            it.key != ArenaSakRegel::class.simpleName &&
            it.key != ManueltOverstyrtTilArenaRegel::class.simpleName
        }

        return reglerTilEvaluering.values.all { it }.also {
            log.info(
                "journalpostId=$forJournalpost skal til Kelvin? $it ${
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
        val regelresultat = hentAktiveRegler(repositoryProvider, gatewayProvider)
            .associate { regel ->
                val regelResultat = regel.vurder(input)
                PrometheusProvider.prometheus.regelresultat(regelResultat, regel.regelNavn()).increment()
                regel.regelNavn() to regel.vurder(input)
            }
            .let {
                Regelresultat(it, input.journalpostId)
            }
        return regelresultat
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
