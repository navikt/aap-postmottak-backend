package no.nav.aap.fordeler.regler

import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Kilde
import no.nav.aap.postmottak.gateway.Periode
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafSak
import no.nav.aap.postmottak.gateway.SakStatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class ArenaHistorikkRegelTest {
    @Test
    fun `Dersom bruker har en sak i Joark med fagsystem AO01 og tema AAP, skal regelen returnere false`() {
        // Arrange
        val input = ArenaHistorikkRegelInput(
            sakerFraArena = emptyList(),
            sakerFraJoark = listOf(
                SafSak(
                    fagsaksystem = "AO01",
                    tema = "AAP"
                ),
                SafSak(
                    fagsaksystem = "AO01",
                    tema = "TSO"
                ),
            )
        )
        val regel = ArenaHistorikkRegel()

        // Act
        val resultat = regel.vurder(input)

        // Assert
        assertFalse(resultat)
    }

    @Test
    fun `Dersom bruker har sak i Arena, skal regelen returnere false`() {
        val journalpostId = JournalpostId(1)
        val person = Person(1, UUID.randomUUID(), listOf(Ident("12345678901")))

        GatewayRegistry.register(JoarkMock::class).register(ApiInternMock::class)
        val regelMedInputGenerator = ArenaHistorikkRegel.medDataInnhenting()
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

    override fun hentAapSakerForPerson(person: Person): List<SakStatus> {
        val fom = LocalDate.of(2020, 1, 1)
        val tom = LocalDate.of(2021, 1, 31)
        return listOf(
            SakStatus(
                sakId = "1",
                vedtakStatusKode = "AVS",
                Periode(fom, tom),
                kilde = Kilde.ARENA
            )
        )
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

    override fun hentSaker(ident: String): List<SafSak> {
        return emptyList()
    }


}