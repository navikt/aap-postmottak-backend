package no.nav.aap.postmottak.fordeler.regler

import no.nav.aap.postmottak.klient.AapInternApiKlient
import no.nav.aap.postmottak.klient.SakStatus
import no.nav.aap.postmottak.klient.joark.Fagsystem
import no.nav.aap.postmottak.saf.graphql.SafGraphqlKlient
import no.nav.aap.postmottak.saf.graphql.SafSak

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
        val sakerFraArena = AapInternApiKlient().hentArenaSakerForPerson(input.person)
        val sakerFraJoark =
            SafGraphqlKlient.withClientCredentialsRestClient().hentSaker(input.person.aktivIdent().identifikator)
        return ArenaSakRegelInput(sakerFraArena, sakerFraJoark)
    }
}

data class ArenaSakRegelInput(
    val sakerFraArena: List<SakStatus>,
    val sakerFraJoark: List<SafSak>
)