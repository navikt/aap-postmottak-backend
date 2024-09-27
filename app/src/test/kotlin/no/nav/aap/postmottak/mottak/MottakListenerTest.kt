package no.nav.aap.postmottak.mottak

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import lib.kafka.StreamsMock
import libs.kafka.SchemaRegistryConfig
import libs.kafka.SslConfig
import libs.kafka.StreamsConfig
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.Test
import javax.sql.DataSource


class MottakListenerTest {

    val behandlingRepository: BehandlingRepository = mockk(relaxed = true)
    val flytJobbRepository: FlytJobbRepository = mockk(relaxed = true)

    @Test
    fun `veridiser mottagelse av joark event og oppretting av behandling og jobb`() {
        val config = config()
        val kafka = setUpStreamsMock(config)
        val topics = Topics(config)

        val hendelseRecord = lagHendelseRecord()

        val behandling = Behandling(
            BehandlingId(1),
            JournalpostId(hendelseRecord.journalpostId)
        )
        every { behandlingRepository.opprettBehandling(any()) } returns behandling

        val journalføringstopic = kafka.testTopic(topics.journalfoering)
        journalføringstopic.produce("1") {
            hendelseRecord
        }

        verify(exactly = 1) {
            behandlingRepository.opprettBehandling(
                JournalpostId(hendelseRecord.journalpostId)
            )
        }
        verify(exactly = 1) { flytJobbRepository.leggTil(any()) }
    }

    private fun setUpStreamsMock(config: StreamsConfig): StreamsMock {
        val kafka = StreamsMock()
        val registry = SimpleMeterRegistry()
        val topology = MottakListener(
            config,
            mockk<DataSource>(relaxed = true),
            {dataSource, fn -> dataSource.transaction { fn(behandlingRepository, flytJobbRepository) }}
        )

        kafka.connect(
            topology = topology(),
            config = config,
            registry = registry,
        )

        return kafka
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