package no.nav.aap.fordeler.regler

import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafSak
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.*

class ArenaHistorikkRegelTest {
    
    @Test
    fun `Dersom bruker har sak i Arena, skal regelen returnere false`() {
        val journalpostId = JournalpostId(1)
        val person = Person(1, UUID.randomUUID(), listOf(Ident("12345678901")))

        GatewayRegistry.register(JoarkMock::class).register(ApiInternMock::class)
        val regelMedInputGenerator = ArenaHistorikkRegel.medDataInnhenting(null, null)
        val res = regelMedInputGenerator.vurder(
            RegelInput(
                person = person,
                journalpostId = journalpostId.referanse,
                brevkode = Brevkoder.SØKNAD.name
            )
        )

        assertFalse(res)
    }
}

class ApiInternMock : AapInternApiGateway {
    companion object : Factory<ApiInternMock> {
        override fun konstruer(): ApiInternMock {
            return ApiInternMock()
        }
    }

    override fun harAapSakIArena(person: Person): PersonEksistererIAAPArena {
        return PersonEksistererIAAPArena(true)
    }
}

class JoarkMock : JournalpostGateway {
    companion object : Factory<JoarkMock> {
        override fun konstruer(): JoarkMock {
            return JoarkMock()
        }
    }

    override fun hentJournalpost(journalpostId: JournalpostId): SafJournalpost {
        TODO("Not yet implemented")
    }

    override fun hentSaker(fnr: String): List<SafSak> {
        return emptyList()
    }


}