package no.nav.aap.fordeler.regler

import io.mockk.mockk
import no.nav.aap.fordeler.regler.ApiInternMock.Companion.identMedSignifikantSak
import no.nav.aap.fordeler.regler.ApiInternMock.Companion.identUtenSignifikantSak
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class SignifikantArenaHistorikkRegelTest {

    @Test
    fun `Dersom bruker har signifikant sak i Arena, skal regelen returnere false`() {
        val journalpostId = JournalpostId(1)
        val person = Person(1, UUID.randomUUID(), listOf(Ident(identMedSignifikantSak)))

        val gatewayRegistry = GatewayRegistry()
            .register<JoarkMock>()
            .register<ApiInternMock>()
        SignifikantArenaHistorikkRegel.innslippProsent = 100 // Sikre at personen tas med i testen
        val regelMedInputGenerator =
            SignifikantArenaHistorikkRegel.medDataInnhenting(
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
        SignifikantArenaHistorikkRegel.innslippProsent = 100 // Sikre at personen tas med i testen
        val regelMedInputGenerator =
            SignifikantArenaHistorikkRegel.medDataInnhenting(
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