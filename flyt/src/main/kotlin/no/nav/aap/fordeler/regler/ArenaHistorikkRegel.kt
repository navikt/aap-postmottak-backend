package no.nav.aap.fordeler.regler

import no.nav.aap.api.intern.Kilde
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.SafSak

class ArenaHistorikkRegel : Regel<ArenaHistorikkRegelInput> {
    companion object : RegelFactory<ArenaHistorikkRegelInput> {
        override val erAktiv = miljøConfig(prod = false, dev = true)
        override fun medDataInnhenting(connection: DBConnection?) =
            RegelMedInputgenerator(ArenaHistorikkRegel(), ArenaSakRegelInputGenerator())
    }

    override fun vurder(input: ArenaHistorikkRegelInput): Boolean {
        // TODO: Avklar om vi skal fjerne denne
        val sakerJournalførtPåArenaAap = input.sakerFraJoark
            .filter { it.fagsaksystem == Fagsystem.AO01.name }
            .filter { it.tema == "AAP" }
        // TODO: Dersom vi skal ha en mildere regel for Arena-historikk må AvklarSakSteg oppdateres */
        return input.sakerFraArena.isEmpty() && sakerJournalførtPåArenaAap.isEmpty()
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ArenaSakRegelInputGenerator : InputGenerator<ArenaHistorikkRegelInput> {
    override fun generer(input: RegelInput): ArenaHistorikkRegelInput {
        val sakerFraArena = GatewayProvider.provide(AapInternApiGateway::class).hentAapSakerForPerson(input.person)
            .filter { it.kilde == Kilde.ARENA }.map { it.sakId }
        val sakerFraJoark =
            GatewayProvider.provide(JournalpostGateway::class).hentSaker(input.person.aktivIdent().identifikator)
        return ArenaHistorikkRegelInput(sakerFraArena, sakerFraJoark)
    }
}

data class ArenaHistorikkRegelInput(
    val sakerFraArena: List<String>,
    val sakerFraJoark: List<SafSak>
)