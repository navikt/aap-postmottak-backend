package no.nav.aap.fordeler.regler

import io.mockk.mockk
import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.SignifikanteSakerResponse
import no.nav.aap.fordeler.regler.ApiInternMock.Companion.identMedSignifikantSak
import no.nav.aap.fordeler.regler.ApiInternMock.Companion.identUtenSignifikantSak
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.gateway.GatewayProvider
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class ArenaHistorikkRegelTest {

    @Test
    fun `Dersom bruker har sak i Arena, skal regelen returnere false`() {
        val journalpostId = JournalpostId(1)
        val person = Person(1, UUID.randomUUID(), listOf(Ident(identMedSignifikantSak)))

        val gatewayRegistry = GatewayRegistry()
            .register<JoarkMock>()
            .register<ApiInternMock>()
        val regelMedInputGenerator =
            ArenaHistorikkRegel.medDataInnhenting(
                mockk(),
                GatewayProvider(gatewayRegistry)
            )
        val res = regelMedInputGenerator.vurder(
            RegelInput(
                journalpostId = journalpostId.referanse,
                person = person,
                brevkode = Brevkoder.SØKNAD.name,
                mottattDato = LocalDate.of(2025, 1, 1)
            )
        )

        assertFalse(res)
    }

    @Test
    fun `Dersom bruker IKKE har signifikant sak i Arena, skal regelen returnere true`() {
        val journalpostId = JournalpostId(1)
        val person = Person(1, UUID.randomUUID(), listOf(Ident(identUtenSignifikantSak)))

        val gatewayRegistry = GatewayRegistry()
            .register<JoarkMock>()
            .register<ApiInternMock>()
        val regelMedInputGenerator =
            ArenaHistorikkRegel.medDataInnhenting(
                mockk(),
                GatewayProvider(gatewayRegistry)
            )
        val res = regelMedInputGenerator.vurder(
            RegelInput(
                journalpostId = journalpostId.referanse,
                person = person,
                brevkode = Brevkoder.SØKNAD.name,
                mottattDato = LocalDate.of(2025, 1, 1)
            )
        )

        assertTrue(res)
    }

}

class ApiInternMock : AapInternApiGateway {
    companion object : Factory<ApiInternMock> {
        override fun konstruer(): ApiInternMock {
            return ApiInternMock()
        }

        const val identUtenSignifikantSak = "ikke_funnet"
        const val identMedSignifikantSak = "12345678901"
    }

    override fun harAapSakIArena(person: Person): PersonEksistererIAAPArena {
        return if (person.identer().first().identifikator == identMedSignifikantSak) {
            PersonEksistererIAAPArena(true)
        } else {
            PersonEksistererIAAPArena(false)
        }
    }

    override fun harSignifikantHistorikkIAAPArena(
        person: Person,
        mottattDato: LocalDate
    ): SignifikanteSakerResponse {
        return if (person.identer().first().identifikator == identMedSignifikantSak) {
            SignifikanteSakerResponse(true, listOf("1234"))
        } else {
            SignifikanteSakerResponse(false, emptyList())
        }
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