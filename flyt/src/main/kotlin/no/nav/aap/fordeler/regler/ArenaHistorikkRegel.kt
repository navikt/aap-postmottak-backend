package no.nav.aap.fordeler.regler

import kotlinx.coroutines.runBlocking
import no.nav.aap.fordeler.arena.ArenaService
import no.nav.aap.fordeler.regler.ArenaHistorikkRegel.Companion.metrikkerForArenaHistorikk
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.PrometheusProvider.Companion.prometheus
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.resultatAvSignifikantArenaHistorikkFilterTeller
import no.nav.aap.postmottak.søknadOmAapTeller
import no.nav.aap.postmottak.tellAntallMaksUtvidetKvoteSnartOppbrukt
import no.nav.aap.postmottak.tellManueltFordeles
import org.slf4j.LoggerFactory

class ArenaHistorikkRegel : Regel<ArenaHistorikkRegelInput> {

    companion object : RegelFactory<ArenaHistorikkRegelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = true)

        override fun medDataInnhenting(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): RegelMedInputgenerator<ArenaHistorikkRegelInput> {
            return RegelMedInputgenerator(
                ArenaHistorikkRegel(),
                ArenaHistorikkRegelInputGenerator(gatewayProvider)
            )
        }

        internal fun metrikkerForArenaHistorikk(
            harArenaHistorikk: Boolean,
            harSignifikantArenaHistorikk: Boolean,
            erSøknad: Boolean
        ) {
            if (harArenaHistorikk) {
                prometheus.resultatAvSignifikantArenaHistorikkFilterTeller(harSignifikantArenaHistorikk).increment()
            }

            prometheus.søknadOmAapTeller(harArenaHistorikk, harSignifikantArenaHistorikk, erSøknad).increment()
        }
    }

    override fun vurder(input: ArenaHistorikkRegelInput): Boolean {
        return !input.harSignifikantHistorikkIAAPArena
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ArenaHistorikkRegelInputGenerator(private val gatewayProvider: GatewayProvider) :
    InputGenerator<ArenaHistorikkRegelInput> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun generer(input: RegelInput): ArenaHistorikkRegelInput {
        val arena = gatewayProvider.provide(ArenaoppslagGateway::class)

        val (historikk, signifikantHistorikk) = runBlocking {
            val historikk = arena.harHistorikk(input.person)
            val signifikantHistorikk = arena.harSignifikantHistorikk(input.person, input.mottattDato)
            historikk to signifikantHistorikk
        }

        val harSignifikantArenaHistorikk = signifikantHistorikk.harSignifikantHistorikk
        val erSøknad = Brevkoder.fraKode(input.brevkode) == Brevkoder.SØKNAD
        metrikkerForArenaHistorikk(
            historikk,
            harSignifikantArenaHistorikk,
            erSøknad
        )

        if (harSignifikantArenaHistorikk) {
            logger.info(
                "Personen har signifikant historikk i AAP-Arena: " +
                        "saker=${signifikantHistorikk}, journalpostId=${input.journalpostId}"
            )
            if (erSøknad) {
                runCatching {
                    // Måles kun, påvirker ikke funksjonaliteten
                    runBlocking {
                        val arenaService = ArenaService(gatewayProvider)
                        val maksKvoteSnartOppbrukt =
                            arenaService.kanFordelesAutomatiskPga11_12_erMakset(
                                input.person, input.mottattDato, input.journalpostId,
                                signifikantHistorikk
                            )
                        prometheus.tellAntallMaksUtvidetKvoteSnartOppbrukt(maksKvoteSnartOppbrukt).increment()

                        if (!maksKvoteSnartOppbrukt) {
                            val skalManueltFordeles = arenaService.skalManueltFordeles(
                                input.person, input.mottattDato, input.journalpostId,
                                signifikantHistorikk
                            )
                            prometheus.tellManueltFordeles(skalManueltFordeles).increment()
                        }
                    }
                }.onFailure { error ->
                    logger.warn("Feil under telling av undergrupper til Arena", error)
                }
            }

        } else {
            logger.info(
                "Personen har /IKKE/ signifikant historikk i AAP-Arena: " +
                        "journalpostId=${input.journalpostId}"
            )
        }

        return ArenaHistorikkRegelInput(harSignifikantArenaHistorikk, input.person)
    }

}

data class ArenaHistorikkRegelInput(
    val harSignifikantHistorikkIAAPArena: Boolean,
    val person: Person
)