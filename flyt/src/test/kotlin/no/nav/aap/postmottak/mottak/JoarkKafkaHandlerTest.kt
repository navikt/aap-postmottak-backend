package no.nav.aap.postmottak.mottak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.test.FakeUnleash
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.mottak.kafka.config.SchemaRegistryConfig
import no.nav.aap.postmottak.mottak.kafka.config.SslConfig
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.getJournalpostId
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.unleash.FeatureToggle
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.util.UUID


object JoarkTemavalgUnleash : UnleashGateway by FakeUnleash {
    override fun isEnabled(featureToggle: FeatureToggle): Boolean =
        featureToggle == PostmottakFeature.PostmottakVelgTema || FakeUnleash.isEnabled(featureToggle)
}

@Fakes
class JoarkKafkaHandlerTest {

    val behandlingRepository: BehandlingRepository = mockk(relaxed = true)
    val flytJobbRepository: FlytJobbRepository = mockk(relaxed = true)
    val repositoryRegistry = mockk<RepositoryRegistry>()
    val avklarTemaRepository = mockk<AvklarTemaRepository>(relaxed = true)
    val journalpostGateway = mockk<JournalpostGateway>()

    @ParameterizedTest
    @ValueSource(strings = ["AAP", "", "BAR"])
    fun `kun reell retur fra annet tema starter ny journalføring`(tidligereTema: String) {
        val avsluttet = Behandling(
            BehandlingId(9L), JournalpostId(123L), Status.AVSLUTTET,
            opprettetTidspunkt = LocalDateTime.now(),
            referanse = BehandlingsreferansePathParam(UUID.randomUUID()),
            typeBehandling = TypeBehandling.Journalføring
        )
        every { behandlingRepository.hentAlleBehandlingerForJournalpost(any()) } returns listOf(avsluttet)
        every { avklarTemaRepository.hentTemaAvklaring(avsluttet.id) } returns TemaVurdering(false, Tema.BAR)
        every { behandlingRepository.opprettBehandling(any(), any()) } returns BehandlingId(10L)

        setUpStreamsMock(config(), velgTema = true) {
            pipeInput("123", lagHendelseRecord(gammeltTema = tidligereTema))
            verify(exactly = if (tidligereTema == "BAR") 1 else 0) {
                behandlingRepository.opprettBehandling(JournalpostId(123L), TypeBehandling.Journalføring)
            }
        }
    }

    @Test
    fun `avsluttet AAP-behandling gjenopprettes ikke`() {
        val avsluttet = Behandling(
            BehandlingId(9L), JournalpostId(123L), Status.AVSLUTTET,
            opprettetTidspunkt = LocalDateTime.now(),
            referanse = BehandlingsreferansePathParam(UUID.randomUUID()),
            typeBehandling = TypeBehandling.Journalføring
        )
        every { behandlingRepository.hentAlleBehandlingerForJournalpost(any()) } returns listOf(avsluttet)
        every { avklarTemaRepository.hentTemaAvklaring(avsluttet.id) } returns TemaVurdering(true, Tema.AAP)
        setUpStreamsMock(config(), velgTema = true) {
            pipeInput("123", lagHendelseRecord(gammeltTema = "BAR"))
            verify(exactly = 0) { behandlingRepository.opprettBehandling(any(), any()) }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["BAR", "JOURNALFOERT"])
    fun `utdaterte AAP-hendelser gjenoppretter ikke behandling`(tilstand: String) {
        val avsluttet = Behandling(
            BehandlingId(9L), JournalpostId(123L), Status.AVSLUTTET,
            opprettetTidspunkt = LocalDateTime.now(),
            referanse = BehandlingsreferansePathParam(UUID.randomUUID()),
            typeBehandling = TypeBehandling.Journalføring
        )
        every { behandlingRepository.hentAlleBehandlingerForJournalpost(any()) } returns listOf(avsluttet)
        every { avklarTemaRepository.hentTemaAvklaring(avsluttet.id) } returns TemaVurdering(false, Tema.BAR)
        setUpStreamsMock(config(), velgTema = true) {
            every { journalpostGateway.hentJournalpost(any()) } returns SafJournalpost(
                123L, tema = if (tilstand == "BAR") "BAR" else "AAP",
                journalstatus = if (tilstand == "BAR") Journalstatus.MOTTATT else Journalstatus.JOURNALFOERT,
                relevanteDatoer = emptyList()
            )
            pipeInput("123", lagHendelseRecord(gammeltTema = "BAR"))
            verify(exactly = 0) { behandlingRepository.opprettBehandling(any(), any()) }
        }
    }

    @Test
    fun `verifiser mottagelse av joark event og oppretting regelfordelingjobb`() {
        val config = config()
        setUpStreamsMock(config) {
            val hendelseRecord = lagHendelseRecord()

            pipeInput("yolo", hendelseRecord)

            Thread.sleep(100)

            verify(exactly = 1) {
                flytJobbRepository.leggTil(withArg {
                    assertThat(it.type()).isEqualTo(VurderRelevantDokumentForAAPJobbUtfører.type)
                    assertThat(it.sakId()).isEqualTo(123L)
                    assertThat(it.getJournalpostId()).isEqualTo(JournalpostId(123L))
                })
            }
            // Behandling opprettes først av VurderRelevantDokumentForAAPJobbUtfører, ikke direkte fra Kafka
            verify(exactly = 0) { behandlingRepository.opprettBehandling(any(), any()) }
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

    private fun setUpStreamsMock(
        config: StreamsConfig,
        velgTema: Boolean = false,
        block: TestInputTopic<String, JournalfoeringHendelseRecord>.() -> Unit
    ) {
        val gatewayProvider = defaultGatewayProvider {
            register(if (velgTema) JoarkTemavalgUnleash::class else FakeUnleash::class)
        }
        val transactionProvider = TransactionProvider(mockk(relaxed = true), repositoryRegistry, gatewayProvider)
        every { behandlingRepository.hentÅpenJournalføringsbehandling(any()) } returns null
        val repositoryProvider = mockk<RepositoryProvider>()
        every { repositoryProvider.provide<BehandlingRepository>() } returns behandlingRepository
        every { repositoryProvider.provide<FlytJobbRepository>() } returns flytJobbRepository
        every { repositoryProvider.provide<JournalpostRepository>() } returns mockk()
        every { repositoryProvider.provide<AvklaringsbehovRepository>() } returns mockk()
        every { repositoryProvider.provide<AvklarTemaRepository>() } returns avklarTemaRepository
        every { repositoryRegistry.provider(any()) } returns repositoryProvider
        every { journalpostGateway.hentJournalpost(any()) } returns SafJournalpost(
            123L, tema = "AAP", journalstatus = Journalstatus.MOTTATT, relevanteDatoer = emptyList()
        )

        val joarkKafkaHandler =
            JoarkKafkaHandler(
                config, mockk(), repositoryRegistry, gatewayProvider, transactionProvider,
                journalpostGateway = journalpostGateway
            )
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
        brokers = "xxx",
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
