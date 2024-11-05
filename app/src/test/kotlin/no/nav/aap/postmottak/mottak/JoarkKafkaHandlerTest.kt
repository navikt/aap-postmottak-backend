package no.nav.aap.postmottak.mottak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.mottak.kafka.config.SchemaRegistryConfig
import no.nav.aap.postmottak.mottak.kafka.config.SslConfig
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.Test
import kotlin.test.Ignore


class JoarkKafkaHandlerTest {

    val behandlingRepository: BehandlingRepository = mockk(relaxed = true)
    val flytJobbRepository: FlytJobbRepository = mockk(relaxed = true)

    @Test
    fun `verifiser mottagelse av joark event og oppretting av behandling og jobb`() {
        val config = config()
        setUpStreamsMock(config) {
            val hendelseRecord = lagHendelseRecord()

            every { behandlingRepository.opprettBehandling(any(), any()) } returns mockk<BehandlingId>(relaxed = true)

            pipeInput("yolo", hendelseRecord)

            Thread.sleep(100)

            verify(exactly = 1) {
                behandlingRepository.opprettBehandling(
                    JournalpostId(hendelseRecord.journalpostId), TypeBehandling.Journalføring
                )
            }
            verify(exactly = 1) { flytJobbRepository.leggTil(any()) }
        }

    }

    @Test
    @Ignore
    fun `verifiser mottak av temaendringer og avlevering`() {
        val config = config()
        setUpStreamsMock(config) {
            val hendelseRecord = lagHendelseRecord(nyttTema = "IKKE AAP", gammeltTema = "AAP")

            every { behandlingRepository.opprettBehandling(any(), any()) } returns mockk<BehandlingId>(relaxed = true)

            pipeInput("yolo", hendelseRecord)

            Thread.sleep(100)

            verify(exactly = 1) { flytJobbRepository.leggTil(any()) }
        }

    }

    private fun setUpStreamsMock(config: StreamsConfig, block: TestInputTopic<String, JournalfoeringHendelseRecord>.() -> Unit) {
        val transactionProvider: TransactionProvider = mockk(relaxed = true)
        every { transactionProvider.inTransaction(any()) } answers { TransactionContext(behandlingRepository, flytJobbRepository).let(firstArg()) }

        val joarkKafkaHandler = JoarkKafkaHandler(config, mockk(), transactionProvider)
        val topologyTestDriver = TopologyTestDriver(joarkKafkaHandler.topology, config.streamsProperties())
        topologyTestDriver.createInputTopic(JOARK_TOPIC, Serdes.String().serializer(), JournalfoeringHendelseAvro(config).avroserdes.serializer())
            .apply(block)
        topologyTestDriver.close()
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

    private fun config() = StreamsConfig(
        applicationId = "",
        brokers = "",
        ssl = SslConfig(
            truststorePath = "",
            keystorePath = "",
            credstorePsw = "",
        ),
        schemaRegistry = SchemaRegistryConfig(
            url = "mock://kafka",
            user = "",
            password = "",
        )
    )


}