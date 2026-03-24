package no.nav.aap.postmottak.mottak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.FakeUnleash
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.mottak.kafka.config.SchemaRegistryConfig
import no.nav.aap.postmottak.mottak.kafka.config.SslConfig
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.prosessering.FeilregistrertJournalpostJobbUtfører
import no.nav.aap.postmottak.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.test.Fakes
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


@Fakes
class JoarkKafkaHandlerTest {

    val behandlingRepository: BehandlingRepository = mockk(relaxed = true)
    val flytJobbRepository: FlytJobbRepository = mockk(relaxed = true)
    val repositoryRegistry = mockk<RepositoryRegistry>()

    @Test
    fun `verifiser mottagelse av joark event og oppretting regelfordelingjobb`() {
        val config = config()
        setUpStreamsMock(config) {
            val hendelseRecord = lagHendelseRecord()

            pipeInput("yolo", hendelseRecord)

            Thread.sleep(100)

            verify(exactly = 1) {
                flytJobbRepository.leggTil(withArg {
                    assertThat(it.type()).isEqualTo(FordelingRegelJobbUtfører.type)
                })
            }
        }

    }

    @Test
    fun `verifiser mottak av temaendringer og avlevering`() {
        val config = config()
        setUpStreamsMock(config) {
            val hendelseRecord = lagHendelseRecord(nyttTema = "IKKE AAP", gammeltTema = "AAP")

            every { behandlingRepository.opprettBehandling(any(), any()) } returns BehandlingId(10L)
            every { behandlingRepository.hentÅpenJournalføringsbehandling(any()) } returns mockk(relaxed = true)

            pipeInput("yolo", hendelseRecord)

            Thread.sleep(100)

            verify(exactly = 1) { flytJobbRepository.leggTil(any()) }
        }

    }

    @Test
    fun `feilregistrert journalpost med åpen behandling trigger prosessering`() {
        val config = config()
        setUpStreamsMock(config) {
            val hendelseRecord = lagHendelseRecord(jpStatus = "FEILREGISTRERT")

            every { behandlingRepository.hentAlleBehandlingerForSak(any()) } returns listOf(mockk(relaxed = true) {
                every { status() } returns mockk {
                    every { erÅpen() } returns true
                }
                every { id } returns mockk {
                    every { id } returns 1L
                }
            })

            pipeInput("yolo", hendelseRecord)

            Thread.sleep(100)

            verify(exactly = 1) {
                flytJobbRepository.leggTil(withArg {
                    assertThat(it.type()).isEqualTo(ProsesserBehandlingJobbUtfører.type)
                })
            }
        }
    }

    @Test
    fun `feilregistrert journalpost med avsluttede behandlinger oppretter FeilregistrertJournalpostJobb`() {
        val config = config()
        setUpStreamsMock(config) {
            val hendelseRecord = lagHendelseRecord(jpStatus = "FEILREGISTRERT")

            every { behandlingRepository.hentAlleBehandlingerForSak(any()) } returns listOf(mockk(relaxed = true) {
                every { status() } returns mockk {
                    every { erÅpen() } returns false
                }
            })

            pipeInput("yolo", hendelseRecord)

            Thread.sleep(100)

            verify(exactly = 1) {
                flytJobbRepository.leggTil(withArg {
                    assertThat(it.type()).isEqualTo(FeilregistrertJournalpostJobbUtfører.type)
                })
            }
        }
    }

    @Test
    fun `feilregistrert journalpost uten behandlinger oppretter ingen jobb`() {
        val config = config()
        setUpStreamsMock(config) {
            val hendelseRecord = lagHendelseRecord(jpStatus = "FEILREGISTRERT")

            every { behandlingRepository.hentAlleBehandlingerForSak(any()) } returns emptyList()

            pipeInput("yolo", hendelseRecord)

            Thread.sleep(100)

            verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
        }
    }

    @Test
    fun `feilregistrert journalpost med annet tema enn AAP ignoreres`() {
        val config = config()
        setUpStreamsMock(config) {
            val hendelseRecord = lagHendelseRecord(jpStatus = "FEILREGISTRERT", nyttTema = "SYK")

            pipeInput("yolo", hendelseRecord)

            Thread.sleep(100)

            verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
            verify(exactly = 0) { behandlingRepository.hentAlleBehandlingerForSak(any()) }
        }
    }

    private fun setUpStreamsMock(
        config: StreamsConfig,
        block: TestInputTopic<String, JournalfoeringHendelseRecord>.() -> Unit
    ) {
        val gatewayProvider = defaultGatewayProvider {
            register(FakeUnleash::class)
        }
        val transactionProvider = TransactionProvider(mockk(relaxed = true), repositoryRegistry, gatewayProvider)
        every { behandlingRepository.hentÅpenJournalføringsbehandling(any()) } returns null
        val repositoryProvider = mockk<RepositoryProvider>()
        every { repositoryProvider.provide<BehandlingRepository>() } returns behandlingRepository
        every { repositoryProvider.provide<FlytJobbRepository>() } returns flytJobbRepository
        every { repositoryProvider.provide<JournalpostRepository>() } returns mockk()
        every { repositoryProvider.provide<AvklaringsbehovRepository>() } returns mockk()
        every { repositoryRegistry.provider(any()) } returns repositoryProvider

        val joarkKafkaHandler =
            JoarkKafkaHandler(config, mockk(), repositoryRegistry, gatewayProvider, transactionProvider)
        val topologyTestDriver = TopologyTestDriver(joarkKafkaHandler.topology, config.streamsProperties())
        topologyTestDriver.createInputTopic(
            JOARK_TOPIC,
            Serdes.String().serializer(),
            JournalfoeringHendelseAvro(config).avroserdes.serializer()
        )
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
