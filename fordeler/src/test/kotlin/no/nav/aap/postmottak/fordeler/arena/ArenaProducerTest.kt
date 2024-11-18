package no.nav.aap.postmottak.fordeler.arena

import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import io.mockk.every
import io.mockk.mockk
import no.nav.aap.postmottak.fordeler.HendelsesRepository
import no.nav.aap.postmottak.fordeler.JoarkHendelse
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream


class ArenaProducerTest {

    val hendelsesRepository: HendelsesRepository = mockk()

    @Test
    fun `forventer å motta en journalpost melding`() {

        val key = "key"
        val value = lagHendelseRecord()

        val mockProducer = MockProducer(true, StringSerializer(), SpecificAvroSerializer<JournalfoeringHendelseRecord>()
            .also { it.configure(mapOf(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://kafka"), false) })

        val arenaVideresneder = ArenaProducer(mockProducer, hendelsesRepository)

        every { hendelsesRepository.hentHendelse(key) } returns JoarkHendelse(key, value.toJson())

        arenaVideresneder.sendJournalpostTilArena(key)

        assertThat(mockProducer.history()).hasSize(1)

        val actualRecord = mockProducer.history().first()

        assertThat(actualRecord.key()).isEqualTo(key)
        assertThat(actualRecord.value()).isEqualTo(value)
    }

    @Test
    fun `fanger og kaster exception videre ved exception fra kafka`() {

        val key = "key"
        val value = lagHendelseRecord().toJson()

        val mockProducer = MockProducer(true, StringSerializer(), SpecificAvroSerializer<JournalfoeringHendelseRecord>())

        val arenaProducer = ArenaProducer(mockProducer, hendelsesRepository)

        every { hendelsesRepository.hentHendelse(key) } returns JoarkHendelse(key, value)
        mockProducer.sendException = SomethingWentHorriblyWrongException()

        assertThrows<TopicWriteException> {
            arenaProducer.sendJournalpostTilArena(key)
        }

    }

    @Test
    fun `fanger og kaster authorizationException videre ved TopicAuthorizationException fra kafka`() {

        val key = "key"
        val value = lagHendelseRecord().toJson()

        val mockProducer = MockProducer(true, StringSerializer(), SpecificAvroSerializer<JournalfoeringHendelseRecord>())

        val arenaVideresneder = ArenaProducer(mockProducer, hendelsesRepository)

        every { hendelsesRepository.hentHendelse(key) } returns JoarkHendelse(key, value)
        mockProducer.sendException = TopicAuthorizationException("Noooo")

        assertThrows<AuthorizationException> {
            arenaVideresneder.sendJournalpostTilArena(key)
        }

    }

    class SomethingWentHorriblyWrongException: RuntimeException()

}

private fun lagHendelseRecord(
    id: String = "1",
    v: Int = 1,
    type: String = "",
    jpId: Long = 123L,
    gammeltTema: String = "AAP",
    nyttTema: String = "AAP",
    jpStatus: String = "MOTTATT",
    kanal: String = "NAV_NO",
    kanalRefId: String = "",
    behandlingTema: String = ""
) = JournalfoeringHendelseRecord.newBuilder().apply {
    hendelsesId = id
    versjon = v
    hendelsesType = type
    journalpostId = jpId
    temaGammelt = gammeltTema
    temaNytt = nyttTema
    journalpostStatus = jpStatus
    mottaksKanal = kanal
    kanalReferanseId = kanalRefId
    behandlingstema = behandlingTema
}.build()

fun JournalfoeringHendelseRecord.toJson(): String {
    val writer = SpecificDatumWriter<JournalfoeringHendelseRecord>(this.schema)
    val outputStream = ByteArrayOutputStream()
    val encoder = EncoderFactory.get().jsonEncoder(this.schema, outputStream)
    writer.write(this, encoder)
    encoder.flush()
    return outputStream.toString()
}