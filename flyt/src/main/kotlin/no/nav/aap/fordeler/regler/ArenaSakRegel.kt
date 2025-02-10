package no.nav.aap.fordeler.regler

import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Kilde
import no.nav.aap.postmottak.gateway.SafSak

class ArenaSakRegel : Regel<ArenaSakRegelInput> {
    companion object : RegelFactory<ArenaSakRegelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = true)
        override fun medDataInnhenting() =
            RegelMedInputgenerator(ArenaSakRegel(), ArenaSakRegelInputGenerator())
    }

    override fun vurder(input: ArenaSakRegelInput): Boolean {
        // TODO: Avklar om vi skal fjerne filter på tema
        val sakerJournalførtPåArenaAap = input.sakerFraJoark
            .filter { it.fagsaksystem == Fagsystem.AO01.name }
            .filter { it.tema == "AAP" }
        // TODO: Avklar om vi kun skal sjekke joark, eller om vi også sjekker på vedtak i arena
        return input.sakerFraArena.isEmpty() && sakerJournalførtPåArenaAap.isEmpty()
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class ArenaSakRegelInputGenerator : InputGenerator<ArenaSakRegelInput> {
    override fun generer(input: RegelInput): ArenaSakRegelInput {
        val sakerFraArena = GatewayProvider.provide(AapInternApiGateway::class).hentAapSakerForPerson(input.person)
            .filter { it.kilde == Kilde.ARENA }.map { it.sakId }
        val sakerFraJoark =
            GatewayProvider.provide(JournalpostGateway::class).hentSaker(input.person.aktivIdent().identifikator)
        return ArenaSakRegelInput(sakerFraArena, sakerFraJoark)
    }
}

data class ArenaSakRegelInput(
    val sakerFraArena: List<String>,
    val sakerFraJoark: List<SafSak>
)