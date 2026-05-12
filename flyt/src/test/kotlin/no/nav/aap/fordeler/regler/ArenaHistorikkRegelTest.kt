package no.nav.aap.fordeler.regler

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.aap.FakeUnleash
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.fordeler.regler.ArenaoppslagGatewayMock.Companion.identHeltUtenSak
import no.nav.aap.fordeler.regler.ArenaoppslagGatewayMock.Companion.identMedSak
import no.nav.aap.fordeler.regler.ArenaoppslagGatewayMock.Companion.identMedSignifikantSak
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafSak
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.LocalDate
import java.util.*

@Execution(ExecutionMode.SAME_THREAD)
class ArenaHistorikkRegelTest {

    @Test
    fun `Dersom bruker har signifikant sak i Arena, skal regelen returnere false`() {
        val journalpostId = JournalpostId(1)
        val person = Person(1, UUID.randomUUID(), listOf(Ident(identMedSignifikantSak)))

        val gatewayRegistry = GatewayRegistry()
            .register<JoarkMock>()
            .register<ArenaoppslagGatewayMock>()
            .register<FakeUnleash>()
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
    fun `Dersom bruker har sak i Arena men ikke har signifikant sak skal regelen returnere true`() {
        val journalpostId = JournalpostId(1)
        val person = Person(1, UUID.randomUUID(), listOf(Ident(identMedSak)))

        val gatewayRegistry = GatewayRegistry()
            .register<JoarkMock>()
            .register<ArenaoppslagGatewayMock>()
            .register<FakeUnleash>()
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

    @Test
    fun `Dersom bruker har ingen sak i Arena skal regelen returnere true`() {
        val journalpostId = JournalpostId(1)
        val person = Person(1, UUID.randomUUID(), listOf(Ident(identHeltUtenSak)))

        val gatewayRegistry = GatewayRegistry()
            .register<JoarkMock>()
            .register<ArenaoppslagGatewayMock>()
            .register<FakeUnleash>()
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

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp() {
            PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        }
    }


}

class ArenaoppslagGatewayMock : ArenaoppslagGateway {
    companion object : Factory<ArenaoppslagGatewayMock> {
        override fun konstruer(): ArenaoppslagGatewayMock {
            return ArenaoppslagGatewayMock()
        }

        const val identHeltUtenSak = "ikke_funnet"
        const val identMedSak = "12345678901"
        const val identMedSignifikantSak = "09876543210"
    }

    override suspend fun harAapSakIArena(person: Person): Boolean {
        val eksisterer = listOf(identMedSak, identMedSignifikantSak).contains(person.identer().first().identifikator)
        return eksisterer
    }

    override suspend fun hentSakerMedSignifikantHistorikk(
        person: Person,
        mottattDato: LocalDate
    ): List<Int> {
        return if (person.identer().first().identifikator == identMedSignifikantSak) {
            listOf(1234)
        } else {
            emptyList()
        }
    }

    override suspend fun harSignifikantHistorikkIAAPArena(
        person: Person,
        mottattDato: LocalDate
    ): Boolean {
        return person.identer().first().identifikator == identMedSignifikantSak
    }

    override suspend fun maksdatoForSaker(ident: Ident): List<SakMedSisteVedtakOgMaksdato> {
        TODO("Not yet implemented")
    }

    override suspend fun sisteUtbetalingsdatoForPerson(ident: Ident): LocalDate? {
        TODO("Not yet implemented")
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