package no.nav.aap.fordeler.regler

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.FakeArenaoppslagGateway.Companion.identHeltUtenSak
import no.nav.aap.postmottak.test.FakeArenaoppslagGateway.Companion.identMedSak
import no.nav.aap.postmottak.test.FakeArenaoppslagGateway.Companion.identMedSignifikantSak
import no.nav.aap.postmottak.test.fakeGatewayProvider
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.LocalDate
import java.util.UUID

@Execution(ExecutionMode.SAME_THREAD)
class ArenaHistorikkRegelTest {

    @Test
    fun `Dersom bruker har signifikant sak i Arena, skal regelen returnere false`() {
        val journalpostId = JournalpostId(1)
        val person = Person(1, UUID.randomUUID(), listOf(Ident(identMedSignifikantSak)))

        val gatewayProvider = fakeGatewayProvider()
        val regelMedInputGenerator =
            ArenaHistorikkRegel.medDataInnhenting(
                mockk(),
                gatewayProvider
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
    fun `Dersom bruker har sak i Arena men ikke har signifikant sak skal regelen returnere true`() {
        val journalpostId = JournalpostId(1)
        val person = Person(1, UUID.randomUUID(), listOf(Ident(identMedSak)))

        val gatewayProvider = fakeGatewayProvider()
        val regelMedInputGenerator =
            ArenaHistorikkRegel.medDataInnhenting(
                mockk(),
                gatewayProvider
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

    @Test
    fun `Dersom bruker har ingen sak i Arena skal regelen returnere true`() {
        val journalpostId = JournalpostId(1)
        val person = Person(1, UUID.randomUUID(), listOf(Ident(identHeltUtenSak)))

        val gatewayProvider = fakeGatewayProvider()
        val regelMedInputGenerator =
            ArenaHistorikkRegel.medDataInnhenting(
                mockk(),
                gatewayProvider
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

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp() {
            PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        }
    }
}
