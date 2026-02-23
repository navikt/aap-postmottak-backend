package no.nav.aap.postmottak.flyt

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.FakeUnleash
import no.nav.aap.WithDependencies
import no.nav.aap.WithDependencies.Companion.repositoryRegistry
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.api.flyt.Venteinformasjon
import no.nav.aap.postmottak.api.flyt.service.RedigitaliseringService
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehov
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.avklaringsbehov.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarSaksnummerLøsning
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarTemaLøsning
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.postmottak.avklaringsbehov.løsning.DigitaliserDokumentLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksinfo
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.flyt.internals.TestHendelsesMottak
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.mottak.JOARK_TOPIC
import no.nav.aap.postmottak.mottak.JoarkKafkaHandler
import no.nav.aap.postmottak.mottak.JournalfoeringHendelseAvro
import no.nav.aap.postmottak.mottak.kafka.MottakStream
import no.nav.aap.postmottak.mottak.kafka.config.SchemaRegistryConfig
import no.nav.aap.postmottak.mottak.kafka.config.SslConfig
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.postmottak.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.DigitaliseringsvurderingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.repository.journalpost.JournalpostRepositoryImpl
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.FakePersoner
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.TestIdenter
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.modell.TestPerson
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import java.lang.Thread.sleep
import java.time.LocalDate
import java.util.*


@Fakes
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class Flyttest : WithDependencies {

    companion object {
        private lateinit var dataSource: TestDataSource

        @Container
        private val kafka = KafkaContainer("apache/kafka-native:4.1.0")

        private val gatewayProvider = defaultGatewayProvider {
            register(FakeUnleash::class)
        }
        private lateinit var hendelsesMottak: TestHendelsesMottak
        private lateinit var motor: Motor
        private lateinit var util: TestUtil

        private lateinit var config: StreamsConfig

        private lateinit var producer: KafkaProducer<String, JournalfoeringHendelseRecord>

        private lateinit var stream: MottakStream


        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            dataSource = TestDataSource()
            hendelsesMottak = TestHendelsesMottak(dataSource, repositoryRegistry, gatewayProvider)
            util = TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron != null }.map { it.type })
            motor =
                Motor(
                    dataSource = dataSource,
                    antallKammer = 2,
                    jobber = ProsesseringsJobber.alle(),
                    repositoryRegistry = repositoryRegistry,
                    gatewayProvider = gatewayProvider
                )
            motor.start()
            PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val admin = AdminClient.create(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers))

            admin.createTopics(listOf(NewTopic(JOARK_TOPIC, 1, 1))).all().get()
            admin.close()

            config = StreamsConfig(
                applicationId = "postmottak",
                brokers = kafka.bootstrapServers,
                ssl = SslConfig(
                    truststorePath = "",
                    keystorePath = "",
                    credstorePsw = "",
                    securityProtocol = "PLAINTEXT"
                ),
                schemaRegistry = SchemaRegistryConfig(url = "mock://dummy", user = "", password = "")
            )


            producer = KafkaProducer<String, JournalfoeringHendelseRecord>(Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().javaClass)
                put(
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    JournalfoeringHendelseAvro(config).avroserdes.serializer().javaClass
                )
                put("schema.registry.url", "mock://dummy")
            })

            stream =
                MottakStream(
                    JoarkKafkaHandler(
                        config,
                        dataSource,
                        repositoryRegistry = repositoryRegistry,
                        gatewayProvider = gatewayProvider,
                        prometheus = PrometheusProvider.prometheus
                    ).topology, config
                )
            stream.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
            producer.close()
            stream.close()
        }
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                DO $$
                DECLARE
                    r RECORD;
                BEGIN
                    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename NOT LIKE 'flyway_%') LOOP
                        EXECUTE 'TRUNCATE TABLE public.' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
                    END LOOP;
                END;
                $$;
            """.trimIndent()
            ).use { it.execute() }
        }
    }

    fun leggJournalpostPåKafka(block: JournalfoeringHendelseRecord.() -> Unit): JournalfoeringHendelseRecord {
        val jRecord = journalfoeringHendelseRecord().apply(block)
        val record = ProducerRecord(JOARK_TOPIC, "key", jRecord)
        producer.send(record)
        producer.flush()
        sleep(200)
        util.ventPåSvar()
        return jRecord
    }

    fun journalfoeringHendelseRecord(): JournalfoeringHendelseRecord {
        return JournalfoeringHendelseRecord().apply {
            hendelsesId = "1"
            hendelsesType = "3"
            journalpostStatus = "MOTTATT"
            temaGammelt = "ds"
            temaNytt = "AAP"
            journalpostId = 0L
            mottaksKanal = "323"
            kanalReferanseId = "323"
            behandlingstema = "2323"
        }
    }

    @Test
    fun `digital søknad blir automatisk behandlet`() {
        val journalpostID = TestJournalposter.DIGITAL_SØKNAD_ID

        leggJournalpostPåKafka {
            journalpostId = journalpostID.referanse
        }

        val behandlinger = prøv {
            alleBehandlingerForJournalpost(journalpostID).also { require(it.size > 1) }
        }

        assertThat(behandlinger).hasSize(2)
            .extracting<TypeBehandling> { it.typeBehandling }
            .containsExactlyInAnyOrder(TypeBehandling.Journalføring, TypeBehandling.DokumentHåndtering)

        val behandling = behandlinger!!.first()

        val åpneAvklaringsbehov = hentAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).isEmpty()
    }

    @Test
    fun fordel() {
        val journalpostId = TestJournalposter.DIGITAL_SØKNAD_ID

        triggFordelingJobb(journalpostId)

        val behandlinger = alleBehandlingerForJournalpost(journalpostId)
        assertThat(behandlinger).isNotEmpty
    }

    @Test
    fun `Helautomatisk flyt for legeerklæring som ikke skal til Kelvin`() {
        val journalpostId = TestJournalposter.LEGEERKLÆRING_IKKE_TIL_KELVIN
        val behandlingId = opprettJournalføringsBehandling(journalpostId)
        triggProsesserBehandling(journalpostId, behandlingId)

        val behandlinger = alleBehandlingerForJournalpost(journalpostId)

        assertThat(behandlinger).hasSize(1)
        assertThat(
            behandlinger.filter { it.typeBehandling == TypeBehandling.Journalføring && it.status() == Status.AVSLUTTET }).hasSize(
            1
        )
    }

    @Test
    fun `Helautomatisk flyt for legeerklæring på trukket søknad som ikke skal til  Kelvin`() {
        val journalpostId = TestJournalposter.LEGEERKLÆRING_TRUKKET_SAK
        val behandlingId = opprettJournalføringsBehandling(journalpostId)

        triggProsesserBehandling(journalpostId, behandlingId)

        val behandlinger = alleBehandlingerForJournalpost(journalpostId)
        assertThat(behandlinger).hasSize(1)
        assertThat(
            behandlinger.filter { it.typeBehandling == TypeBehandling.Journalføring && it.status() == Status.AVSLUTTET }).hasSize(
            1
        )
    }

    @Test
    fun `Helautomatisk flyt for digital legeerklæring som skal til Kelvin`() {
        val journalpostId = TestJournalposter.LEGEERKLÆRING
        val behandlingId = opprettJournalføringsBehandling(journalpostId)

        dataSource.transaction { connection ->
            SaksnummerRepositoryImpl(connection).lagreKelvinSak(
                behandlingId, listOf(
                    Saksinfo(
                        "sak: 1", Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 31)),
                    )
                )
            )
        }
        triggProsesserBehandling(journalpostId, behandlingId)
        sleep(100) // FIXME ustabil test uten denne

        val behandlinger = alleBehandlingerForJournalpost(journalpostId)
        assertThat(behandlinger).hasSize(2)
        assertThat(
            behandlinger.filter { it.typeBehandling == TypeBehandling.Journalføring && it.status() == Status.AVSLUTTET }).hasSize(
            1
        )
        assertThat(
            behandlinger.filter { it.typeBehandling == TypeBehandling.DokumentHåndtering && it.status() == Status.AVSLUTTET }).hasSize(
            1
        )
    }

    @Test
    fun `Flyt for journalpost som er blitt behandlet i gosys`() {
        val journalpostId = TestJournalposter.ANNET_TEMA
        val behandlingId = opprettJournalføringsBehandling(journalpostId)

        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            repositoryProvider.provide(AvklarTemaRepository::class).lagreTemaAvklaring(behandlingId, false, Tema.UKJENT)
        }

        triggProsesserBehandling(journalpostId, behandlingId)

        val behandling = hentBehandling(behandlingId)
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        val jobber: List<String> = dataSource.transaction { connection ->
            connection.queryList("""SELECT * FROM behandling WHERE type = 'DokumentHåndtering'""") {
                setRowMapper { row -> row.getString("type") }
            }
        }

        assertThat(jobber).hasSize(0)
    }

    @Test
    fun `Full helautomatisk flyt for søknad`() {
        val journalpostId = TestJournalposter.DIGITAL_SØKNAD_ID
        val behandlingId = opprettJournalføringsBehandling(journalpostId)
        triggProsesserBehandling(journalpostId, behandlingId)
        val behandlinger = alleBehandlingerForJournalpost(journalpostId)

        assertThat(behandlinger).allMatch { it.status() == Status.AVSLUTTET }
    }


    @Test
    fun `kjører en manuell søknad igjennom hele flyten`() {
        val journalpostId = JournalpostId(1)

        val behandlingId = opprettJournalføringsBehandling(journalpostId)

        triggProsesserBehandling(journalpostId, behandlingId)

        val behandling = hentBehandling(behandlingId)
            .løsAvklaringsBehov(AvklarTemaLøsning(skalTilAap = true))
            .løsAvklaringsBehov(AvklarSaksnummerLøsning(saksnummer = "23452345"))
            .sjekkÅpentAvklaringsbehov(null)

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `manuell digitalisering - først opprettes avklartema-behandling, så digitaliseringsbehandling`() {
        val journalpostId = TestJournalposter.PAPIR_SØKNAD

        leggJournalpostPåKafka { this.journalpostId = journalpostId.referanse }

        val behandlinger = prøv {
            alleBehandlingerForJournalpost(journalpostId).also { require(it.isNotEmpty()) }
        }!!

        assertThat(behandlinger).hasSize(1)
        var behandling = behandlinger.first()
        val behandlingId = behandling.id

        util.ventPåSvar(journalpostId.referanse, behandlingId.id)

        sjekkÅpentAvklaringsbehov(behandlingId, Definisjon.AVKLAR_TEMA)
        behandling = behandling
            .løsAvklaringsBehov(AvklarTemaLøsning(skalTilAap = true))
            .løsAvklaringsBehov(AvklarSaksnummerLøsning(saksnummer = "123"))

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
        util.ventPåSvar(journalpostId.referanse)

        val behandlinger2 = prøv {
            alleBehandlingerForJournalpost(journalpostId).also { require(it.isNotEmpty()) }
        }!!

        assertThat(behandlinger2).hasSize(2)

        var behandling2 = behandlinger2.first { it.id != behandlingId }
        val behandling2Id = behandling2.id

        sjekkÅpentAvklaringsbehov(behandling2Id, Definisjon.DIGITALISER_DOKUMENT)

        behandling2 = behandling2.løsAvklaringsBehov(
            DigitaliserDokumentLøsning(
                kategori = InnsendingType.SØKNAD,
                strukturertDokument = DefaultJsonMapper.toJson(
                    SøknadV0(
                        student = null,
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = null,
                    )
                ),
                søknadsdato = LocalDate.now().withYear(2019)
            )
        )

        sjekkÅpentAvklaringsbehov(behandling2Id, null)
        assertThat(behandling2.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `manuell digitalisering skal markere hendelsen før videresending`() {
        val journalpostId = TestJournalposter.PAPIR_SØKNAD

        leggJournalpostPåKafka { this.journalpostId = journalpostId.referanse }

        val behandlinger = prøv {
            alleBehandlingerForJournalpost(journalpostId).also { require(it.isNotEmpty()) }
        }!!

        val behandling = behandlinger.first()
        val behandlingId = behandling.id

        util.ventPåSvar(journalpostId.referanse, behandlingId.id)

        sjekkÅpentAvklaringsbehov(behandlingId, Definisjon.AVKLAR_TEMA)
        behandling
            .løsAvklaringsBehov(AvklarTemaLøsning(skalTilAap = true))
            .løsAvklaringsBehov(AvklarSaksnummerLøsning(saksnummer = "123"))

        util.ventPåSvar(journalpostId.referanse)

        val behandlinger2 = prøv {
            alleBehandlingerForJournalpost(journalpostId).also { require(it.isNotEmpty()) }
        }!!

        var behandling2 = behandlinger2.first { it.id != behandlingId }
        val behandling2Id = behandling2.id

        sjekkÅpentAvklaringsbehov(behandling2Id, Definisjon.DIGITALISER_DOKUMENT)

        behandling2 = behandling2.løsAvklaringsBehov(
            DigitaliserDokumentLøsning(
                kategori = InnsendingType.SØKNAD,
                strukturertDokument = DefaultJsonMapper.toJson(
                    SøknadV0(
                        student = null,
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = null,
                    )
                ),
                søknadsdato = LocalDate.now().withYear(2019)
            )
        )

        dataSource.transaction {
            val vurdering = DigitaliseringsvurderingRepositoryImpl(it).hentHvisEksisterer(behandling2.id)
            assertThat(vurdering?.digitalisertManueltGjennomPostmottak).isTrue()
        }
    }

    @Test
    fun `Forventer at en fordelerjobb oppretter en journalføringsbehandling`() {
        val journalpostId = JournalpostId(1L)

        triggFordelingJobb(journalpostId)

        val alleBehandlinger = alleBehandlingerForJournalpost(journalpostId)
        val behandling = alleBehandlinger
            .find { it.typeBehandling == TypeBehandling.Journalføring }!!

        assertNotNull(behandling)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        assertThat(behandling.journalpostId).isEqualTo(journalpostId)
    }

    @Test
    fun `Blir satt på vent for etterspørring av informasjon`() {
        val journalpostId = JournalpostId(2L)
        val behandlingId = opprettJournalføringsBehandling(journalpostId)

        triggProsesserBehandling(journalpostId, behandlingId)

        hentBehandling(behandlingId)
            .løsAvklaringsBehov(AvklarTemaLøsning(skalTilAap = true))
            .løsAvklaringsBehov(AvklarSaksnummerLøsning(saksnummer = "23452345"))

        val alleBehandlinger = alleBehandlingerForJournalpost(journalpostId)
        var behandling = alleBehandlinger
            .find { it.typeBehandling == TypeBehandling.DokumentHåndtering }!!

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        val alleAvklaringsbehov = dataSource.transaction(readOnly = true) { connection ->
            hentAvklaringsbehov(behandling.id, connection).alle()
        }
        assertThat(alleAvklaringsbehov)
            .extracting(Avklaringsbehov::erÅpent, Avklaringsbehov::definisjon)
            .containsExactly(tuple(true, Definisjon.DIGITALISER_DOKUMENT))

        hendelsesMottak.håndtere(
            behandling.id, BehandlingSattPåVent(
                frist = null,
                begrunnelse = "Avventer dokumentasjon",
                bruker = SYSTEMBRUKER,
                behandlingVersjon = behandling.versjon,
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
            )
        )

        val dto = dataSource.transaction(readOnly = true) { connection ->
            val avklaringsbehovene = hentAvklaringsbehov(behandling.id, connection)

            if (avklaringsbehovene.erSattPåVent()) {
                val avklaringsbehov = avklaringsbehovene.hentVentepunkter().first()
                Venteinformasjon(avklaringsbehov.frist(), avklaringsbehov.begrunnelse(), avklaringsbehov.grunn())
            } else {
                null
            }
        }
        assertThat(dto).isNotNull
        assertThat(dto?.frist).isNotNull

        val alleAvklaringsbehov2 = dataSource.transaction(readOnly = true) { connection ->
            hentAvklaringsbehov(behandling.id, connection).alle()
        }
        assertThat(alleAvklaringsbehov2)
            .extracting(Avklaringsbehov::erÅpent, Avklaringsbehov::definisjon)
            .contains(tuple(true, Definisjon.MANUELT_SATT_PÅ_VENT))

        behandling = hentBehandling(behandling.id)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        val alleAvklaringsbehov3 = dataSource.transaction(readOnly = true) { connection ->
            hentAvklaringsbehov(behandling.id, connection).alle()
        }

        assertThat(alleAvklaringsbehov3).anySatisfy { !it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT }

    }

    @Test
    fun `Skal ikke opprette dokumentflyt dersom journalposten har ugyldig status`() {
        val journalpostId = TestJournalposter.UGYLDIG_STATUS
        val behandlingId = opprettJournalføringsBehandling(journalpostId)

        triggProsesserBehandling(journalpostId, behandlingId)

        val behandling = hentBehandling(behandlingId)
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        val jobber: List<String> = dataSource.transaction {
            it.queryList("""SELECT * FROM behandling WHERE type = 'DokumentHåndtering'""") {
                setRowMapper { row -> row.getString("type") }
            }
        }
        assertThat(jobber).hasSize(0)
    }

    @Test
    fun `Skal videresende dersom journalposten ble journalført utenfor postmottak med tema AAP på Kelvin fagsak `() {
        val journalpostId = TestJournalposter.STATUS_JOURNALFØRT
        val behandlingId = opprettJournalføringsBehandling(journalpostId)
        triggProsesserBehandling(journalpostId, behandlingId)

        val behandlinger = alleBehandlingerForJournalpost(journalpostId)
        assertThat(
            behandlinger.filter { it.typeBehandling == TypeBehandling.Journalføring && it.status() == Status.AVSLUTTET }).hasSize(
            1
        )

    }

    @Test
    fun `Skal ikke videresende dersom journalposten ble journalført utenfor postmottak med tema AAP, men på annet fagsystem`() {
        val journalpostId = TestJournalposter.STATUS_JOURNALFØRT_ANNET_FAGSYSTEM
        val behandlingId = opprettJournalføringsBehandling(journalpostId)

        triggProsesserBehandling(journalpostId, behandlingId)

        val behandlinger = alleBehandlingerForJournalpost(journalpostId)
        assertThat(behandlinger).hasSize(1)
        assertThat(
            behandlinger.filter { it.typeBehandling == TypeBehandling.Journalføring && it.status() == Status.AVSLUTTET }).hasSize(
            1
        )
    }

    @Test
    fun `hvis tema ikke skal være AAP, så lukkes behandlingen når journalposten endrer tema`(fakePersoner: FakePersoner) {
        val testPerson = TestPerson(identer = setOf(TestIdenter.DEFAULT_IDENT))
        fakePersoner.leggTil(testPerson)

        val journalpostId = JournalpostId(12213123L)

        // Legg til journalpost på Kafka
        leggJournalpostPåKafka {
            this.journalpostId = journalpostId.referanse
        }

        val behandlinger = prøv {
            alleBehandlingerForJournalpost(journalpostId).also { require(it.isNotEmpty()) }
        }!!

        assertThat(behandlinger).hasSize(1)
        var behandling = behandlinger.first()
        val behandlingId = behandling.id

        util.ventPåSvar(journalpostId.referanse, behandlingId.id)

        // Verifiser at vi stopper på AVKLAR_TEMA
        sjekkÅpentAvklaringsbehov(behandlingId, Definisjon.AVKLAR_TEMA)

        behandling = behandling
            .løsAvklaringsBehov(AvklarTemaLøsning(skalTilAap = false))
            .sjekkÅpentAvklaringsbehov(Definisjon.AVKLAR_TEMA)
            .verifiserErPåVent {
                assertThat(it).containsExactly(Definisjon.VENT_PA_GOSYS)
            }

        // Samme journalpost kommer tilbake med tema AAP
        leggJournalpostPåKafka {
            this.journalpostId = journalpostId.referanse
            this.temaNytt = "AAP"
        }

        behandling
            .sjekkÅpentAvklaringsbehov(Definisjon.AVKLAR_TEMA)
            .verifiserIkkePåVent()
            .løsAvklaringsBehov(AvklarTemaLøsning(skalTilAap = true))
            .løsAvklaringsBehov(
                AvklarSaksnummerLøsning(saksnummer = "ABC")
            )
            .sjekkÅpentAvklaringsbehov(null)
            .apply {
                assertThat(this.status()).isEqualTo(Status.AVSLUTTET)
            }
    }

    @Test
    fun `hvis tema ikke skal være AAP, så lukkes behandlingen når journalposten fullføres`(fakePersoner: FakePersoner) {
        val testPerson = TestPerson(identer = setOf(TestIdenter.DEFAULT_IDENT))
        fakePersoner.leggTil(testPerson)

        val journalpostId = JournalpostId(12213123L)

        // Legg til journalpost på Kafka
        leggJournalpostPåKafka {
            this.journalpostId = journalpostId.referanse
        }

        val behandlinger = prøv {
            alleBehandlingerForJournalpost(journalpostId).also { require(it.isNotEmpty()) }
        }!!

        assertThat(behandlinger).hasSize(1)
        var behandling = behandlinger.first()
        val behandlingId = behandling.id

        util.ventPåSvar(journalpostId.referanse, behandlingId.id)

        // Verifiser at vi stopper på AVKLAR_TEMA
        sjekkÅpentAvklaringsbehov(behandlingId, Definisjon.AVKLAR_TEMA)

        behandling = behandling
            .løsAvklaringsBehov(AvklarTemaLøsning(skalTilAap = false))
            .sjekkÅpentAvklaringsbehov(Definisjon.AVKLAR_TEMA)
            .verifiserErPåVent {
                assertThat(it).containsExactly(Definisjon.VENT_PA_GOSYS)
            }

        // Simuler at journalposten er journalført utenfor Kelvin
        // (i dag får vi ikke disse fra Kafka)
        dataSource.transaction {
            val eksisterende = JournalpostRepositoryImpl(it).hentHvisEksisterer(journalpostId)
            JournalpostRepositoryImpl(it).lagre(
                eksisterende!!.copy(status = Journalstatus.JOURNALFOERT)
            )
        }

        triggProsesserBehandling(journalpostId, behandlingId)

        behandling
            .sjekkÅpentAvklaringsbehov(Definisjon.AVKLAR_TEMA)
            .løsAvklaringsBehov(AvklarTemaLøsning(skalTilAap = true))
            .løsAvklaringsBehov(
                AvklarSaksnummerLøsning(saksnummer = "ABC")
            )
            .sjekkÅpentAvklaringsbehov(null)
            .apply {
                assertThat(this.status()).isEqualTo(Status.AVSLUTTET)
            }
    }

    @Test
    fun `kan redigitalisere dokument eksisterende i postmottak`() {
        val journalpostId = TestJournalposter.STATUS_JOURNALFØRT
        val behandlingId = opprettJournalføringsBehandling(journalpostId)
        triggProsesserBehandling(journalpostId, behandlingId)

        val behandlinger = alleBehandlingerForJournalpost(journalpostId)
        assertThat(
            behandlinger.filter { it.typeBehandling == TypeBehandling.Journalføring && it.status() == Status.AVSLUTTET }).hasSize(
            1
        )
        util.ventPåSvar()

        dataSource.transaction { connection ->
            val behandling = hentBehandling(behandlingId)
            val redigitaliseringService = RedigitaliseringService.konstruer(repositoryRegistry.provider(connection))
            redigitaliseringService.Redigitaliser(behandling.journalpostId.referanse)
        }
        util.ventPåSvar()

        val redigitalisering = alleBehandlingerForJournalpost(journalpostId)
        val avklaringsbehov = hentAvklaringsbehov(redigitalisering.last())

        assertThat(avklaringsbehov.all { it.definisjon == Definisjon.DIGITALISER_DOKUMENT }).isTrue()
        assertThat(redigitalisering).hasSize(2)
        assertThat(redigitalisering.last().status()).isEqualTo(Status.UTREDES)
        assertThat(redigitalisering.last().typeBehandling).isEqualTo(TypeBehandling.DokumentHåndtering)
    }

    private fun <R> prøv(maksSekunder: Long = 10, block: () -> R): R? {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < maksSekunder * 1000) {
            try {
                return block()
            } catch (_: Throwable) {
                sleep(100)
            }
        }
        return null
    }

    private fun sjekkÅpentAvklaringsbehov(behandlingId: BehandlingId, behov: Definisjon?) {
        val åpneBehov = dataSource.transaction { connection ->
            hentAvklaringsbehov(behandlingId, connection).åpne()
        }

        if (behov == null) {
            assertThat(åpneBehov).isEmpty()
        } else {
            assertThat(åpneBehov).extracting<Definisjon> { it.definisjon }.contains(behov)
        }
    }

    private fun Behandling.sjekkÅpentAvklaringsbehov(behov: Definisjon?): Behandling {
        sjekkÅpentAvklaringsbehov(this.id, behov)
        return this
    }

    fun Behandling.verifiserErPåVent(definisjon: (List<Definisjon>) -> Unit = {}): Behandling {
        val åpneVenteBehov = dataSource.transaction { connection ->
            hentAvklaringsbehov(this.id, connection).hentVentepunkter()
        }
        assertThat(åpneVenteBehov).isNotEmpty()
        definisjon(åpneVenteBehov.map(Avklaringsbehov::definisjon))
        return this
    }

    fun Behandling.verifiserIkkePåVent(): Behandling {
        val åpneVenteBehov = dataSource.transaction { connection ->
            hentAvklaringsbehov(this.id, connection).hentVentepunkter()
        }
        assertThat(åpneVenteBehov).isEmpty()
        return this
    }

    private fun alleBehandlingerForJournalpost(journalpostId: JournalpostId): List<Behandling> =
        dataSource.transaction(readOnly = true) {
            BehandlingRepositoryImpl(it).hentAlleBehandlingerForSak(journalpostId)
        }

    private fun triggProsesserBehandling(
        journalpostId: JournalpostId,
        behandlingId: BehandlingId
    ) {
        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
        }
        util.ventPåSvar(journalpostId.referanse, behandlingId.id, maxTid = 50)
    }

    private fun opprettJournalføringsBehandling(journalpostId: JournalpostId): BehandlingId =
        dataSource.transaction { connection ->
            repositoryRegistry.provider(connection).provide(BehandlingRepository::class)
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
        }

    private fun triggFordelingJobb(journalpostId: JournalpostId) {
        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(FordelingRegelJobbUtfører).forSak(journalpostId.referanse).medJournalpostId(journalpostId)
                    .medCallId()
            )
        }
        util.ventPåSvar(journalpostId.referanse)
    }

    private fun hentBehandling(behandlingId: BehandlingId): Behandling =
        dataSource.transaction(readOnly = true) { connection ->
            repositoryRegistry.provider(connection).provide(BehandlingRepository::class).hent(behandlingId)
        }


    private fun hentAvklaringsbehov(behandlingId: BehandlingId, connection: DBConnection): Avklaringsbehovene {
        return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
    }

    private fun hentAvklaringsbehov(behandling: Behandling): List<Avklaringsbehov> {
        return dataSource.transaction(readOnly = true) { connection ->
            hentAvklaringsbehov(behandling.id, connection).åpne()
        }
    }

    private fun Behandling.løsAvklaringsBehov(
        avklaringsBehovLøsning: AvklaringsbehovLøsning,
        bruker: Bruker = Bruker("SAKSBEHANDLER"),
        ingenEndringIGruppe: Boolean = false
    ): Behandling {
        dataSource.transaction {
            AvklaringsbehovHendelseHåndterer(
                BehandlingRepositoryImpl(it),
                AvklaringsbehovRepositoryImpl(it),
                AvklaringsbehovOrkestrator(postgresRepositoryRegistry.provider(it), gatewayProvider),
            ).håndtere(
                this.id, LøsAvklaringsbehovBehandlingHendelse(
                    løsning = avklaringsBehovLøsning,
                    behandlingVersjon = this.versjon,
                    bruker = bruker,
                    ingenEndringIGruppe = ingenEndringIGruppe
                )
            )
        }
        util.ventPåSvar(this.journalpostId.referanse, this.id.id)
        return hentBehandling(this.id)
    }

}
