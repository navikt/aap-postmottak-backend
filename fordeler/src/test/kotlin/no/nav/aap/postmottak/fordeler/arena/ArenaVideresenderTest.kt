package no.nav.aap.postmottak.fordeler.arena

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.postmottak.fordeler.HendelsesRepository
import no.nav.aap.postmottak.fordeler.JoarkHendelse
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class ArenaVideresenderTest {

    val hendelsesRepository: HendelsesRepository = mockk()

    @Test
    fun `forventer å motta en journalport melding`() {

        val key = "key"
        val value = "value"

        val mockProducer = MockProducer(true, StringSerializer(), StringSerializer())
        val arenaVideresneder = ArenaVideresender(mockProducer, hendelsesRepository)

        every { hendelsesRepository.hentHendelse(key) } returns JoarkHendelse(key, value)

        arenaVideresneder.sendJournalpostTilArena(key)

        assertThat(mockProducer.history()).hasSize(1)

        val actualRecord = mockProducer.history().first()

        assertThat(actualRecord.key()).isEqualTo(key)
        assertThat(actualRecord.value()).isEqualTo(value)
    }

}