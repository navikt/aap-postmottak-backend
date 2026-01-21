package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person

class SignifikantArenaHistorikkRegel : Regel<SignifikantArenaHistorikkRegelInput> {
    private object RateBegrenser {
        @SuppressWarnings("MagicNumber")
        fun personenTasMed(person: Person): Boolean {
            val personId = person.identer().first()
            val fordelingsTall = personId.hashCode() % 100 + 1 // gir verdier 1..100
            return innslippProsent >= fordelingsTall
        }
    }

    companion object : RegelFactory<SignifikantArenaHistorikkRegelInput> {
        override val erAktiv = miljøConfig(prod = false, dev = true)
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            RegelMedInputgenerator(
                SignifikantArenaHistorikkRegel(),
                SignifikantArenaHistorikkRegelInputGenerator(gatewayProvider)
            )

        var innslippProsent: Int = 25
    }

    override fun vurder(input: SignifikantArenaHistorikkRegelInput): Boolean {
        return !input.harSignifikantHistorikkIAAPArena && RateBegrenser.personenTasMed(input.person)
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}

class SignifikantArenaHistorikkRegelInputGenerator(private val gatewayProvider: GatewayProvider) :
    InputGenerator<SignifikantArenaHistorikkRegelInput> {
    override fun generer(input: RegelInput): SignifikantArenaHistorikkRegelInput {
        val apiInternGateway = gatewayProvider.provide(AapInternApiGateway::class)
        val response = apiInternGateway.harSignifikantHistorikkIAAPArena(input.person, input.mottattDato)
        return SignifikantArenaHistorikkRegelInput(response.harSignifikantHistorikk, input.person)
    }
}

data class SignifikantArenaHistorikkRegelInput(
    val harSignifikantHistorikkIAAPArena: Boolean,
    val person: Person
)