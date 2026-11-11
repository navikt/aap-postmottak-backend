# Test templates

Templates 1, 2, 4, 5 and 6 were compiled and run green against this repo (October 2026); template 3 is a skeleton with placeholder names. Copy the imports from the referenced files.

## 1. Step test, fake-first (preferred for step logic)

Real step wiring, real Postgres, real HTTP clients against fakes. Living example: `flyt/src/test/kotlin/no/nav/aap/postmottak/forretningsflyt/steg/journalføring/AvklarSakStegMedFakesTest.kt`.

```kotlin
@Fakes
class MittStegTest {
    private lateinit var dataSource: TestDataSource
    private val gatewayProvider = defaultGatewayProvider { register<FakeUnleash>() }

    @BeforeEach fun setup() { dataSource = TestDataSource() }   // fresh database per test (cloned template)
    @AfterEach fun tearDown() = dataSource.close()

    @Test
    fun `digital søknad knyttes automatisk til Kelvin-sak`() {
        val person = TestPersoner.leggTil { kelvinSak = TestKelvinSak(saksnummer = "S-123") }
        val journalpostId = TestJournalposter.leggTil { this.person = person; digitalSøknad() }.journalpostId()

        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            val behandlingId = repositoryProvider.provide<BehandlingRepository>()
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            val kontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)

            // Seed the journalpost exactly like OPPDATER_FAKTAGRUNNLAG does (reads the SAF fake, upserts the person).
            JournalpostInformasjonskrav.konstruer(repositoryProvider, gatewayProvider).oppdater(kontekst)

            val resultat = AvklarSakSteg.konstruer(repositoryProvider, gatewayProvider).utfør(kontekst)

            assertThat(resultat).isEqualTo(Fullført)
            assertThat(repositoryProvider.provide<SaksnummerRepository>().hentSakVurdering(behandlingId)?.saksnummer)
                .isEqualTo("S-123")
        }
    }
}
```

For "needs manual handling" assert `isInstanceOf(FantAvklaringsbehov::class.java)` and read the definisjon with `(resultat.transisjon() as FunnetAvklaringsbehov).avklaringsbehov()`. To test the re-run after a løsning, store the vurdering (or run the løser, template 2) and call `utfør` again.

## 2. Løser test, fake-first

Same setup as template 1, then:

```kotlin
AvklarSakLøser.konstruer(repositoryProvider, gatewayProvider).løs(
    AvklaringsbehovKontekst(Bruker("Z999999"), kontekst),
    AvklarSaksnummerLøsning(saksnummer = "S-1", journalposttittel = "Søknad om AAP"),
)
val vurdering = repositoryProvider.provide<SaksnummerRepository>().hentSakVurdering(behandlingId)
assertThat(vurdering?.saksnummer).isEqualTo("S-1")
```

Validation in løsere is mostly `require(...)`: test rejected input with `assertThrows<IllegalArgumentException> { ... }` and assert that nothing was stored.

## 3. Repository test

Pattern from `repository/src/test/.../BehandlingRepositoryImplTest.kt` and `SaksnummerRepositoryImplTest.kt`:

```kotlin
class MinRepositoryImplTest {
    private lateinit var dataSource: TestDataSource
    @BeforeEach fun setup() { dataSource = TestDataSource() }
    @AfterEach fun tearDown() = dataSource.close()

    @Test
    fun `lagrer, henter og kopierer vurdering`() {
        dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val repository = MinRepositoryImpl(connection)
            val fra = behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring)
            val til = behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.DokumentHåndtering)

            repository.lagre(fra, MinVurdering(...))
            repository.kopier(fra, til)

            assertThat(repository.hentHvisEksisterer(til)).isEqualTo(repository.hentHvisEksisterer(fra))
        }
    }
}
```

Cover: save twice (only the last is active), `kopier`, and a behandling without data (`null`).

## 4. Kafka topology test against a real database

Runs the real `JoarkKafkaHandler` topology with a `TopologyTestDriver` (no broker) and real repositories. The existing `JoarkKafkaHandlerTest` does the same with mocked repositories.

```kotlin
@Fakes
class MinKafkaTest {
    private lateinit var dataSource: TestDataSource
    private val gatewayProvider = defaultGatewayProvider { register<FakeUnleash>() }
    private val config = StreamsConfig(
        applicationId = "postmottak-test",
        brokers = "localhost:9092", // never contacted, but TopologyTestDriver rejects an empty value
        ssl = SslConfig(truststorePath = "", keystorePath = "", credstorePsw = ""),
        schemaRegistry = SchemaRegistryConfig(url = "mock://postmottak-test", user = "", password = ""),
    )

    @BeforeEach fun setup() { dataSource = TestDataSource() }
    @AfterEach fun tearDown() = dataSource.close()

    @Test
    fun `ny AAP-journalpost gir vurderingsjobb`() {
        val journalpostId = TestJournalposter.digitalSøknad().journalpostId()

        val handler = JoarkKafkaHandler(config, dataSource, postgresRepositoryRegistry, gatewayProvider)
        TopologyTestDriver(handler.topology, config.streamsProperties()).use { driver ->
            driver.createInputTopic(JOARK_TOPIC, Serdes.String().serializer(), JournalfoeringHendelseAvro(config).avroserdes.serializer())
                .pipeInput("key", JournalfoeringHendelseRecord.newBuilder().apply {
                    hendelsesId = "1"; versjon = 1; hendelsesType = "JournalpostMottatt"
                    this.journalpostId = journalpostId.referanse
                    temaGammelt = "AAP"; temaNytt = "AAP"; journalpostStatus = "MOTTATT"
                    mottaksKanal = "NAV_NO"; kanalReferanseId = ""; behandlingstema = ""
                }.build())
        }

        // Plain SQL: FlytJobbRepository.hentJobber* only works after a Motor has registered the job types.
        val jobbtyper = dataSource.transaction { connection ->
            connection.queryList("SELECT type FROM jobb WHERE sak_id = ?") {
                setParams { setLong(1, journalpostId.referanse) }
                setRowMapper { it.getString("type") }
            }
        }
        assertThat(jobbtyper).containsExactly(VurderRelevantDokumentForAAPJobbUtfører.type)
    }
}
```

All fields of `JournalfoeringHendelseRecord` without a default must be set before `build()`.

## 5. Motor job chain (jobs run for real, no Kafka)

Pattern from `fordeler/arena/ArenaOppgaveFlytTest.kt`. One motor and database per class:

```kotlin
@Fakes
class MinJobbkjedeTest : WithDependencies {
    companion object {
        private val gatewayProvider = defaultGatewayProvider { register<FakeUnleash>() }
        private lateinit var dataSource: TestDataSource
        private lateinit var motor: Motor
        private lateinit var util: TestUtil

        @JvmStatic @BeforeAll
        fun beforeAll() {
            dataSource = TestDataSource()
            motor = Motor(dataSource = dataSource, antallKammer = 2, jobber = ProsesseringsJobber.alle(),
                repositoryRegistry = postgresRepositoryRegistry, gatewayProvider = gatewayProvider)
            motor.start()
            util = TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron != null }.map { it.type })
        }

        @JvmStatic @AfterAll
        fun afterAll() { motor.stop(); dataSource.close() }
    }

    @Test
    fun `digital søknad fra person med Kelvin-sak går helautomatisk gjennom alle tre behandlinger`() {
        val person = TestPersoner.leggTil { kelvinSak = TestKelvinSak() }
        val journalpostId = TestJournalposter.leggTil { this.person = person; digitalSøknad() }.journalpostId()

        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(VurderRelevantDokumentForAAPJobbUtfører).forSak(journalpostId.referanse).medJournalpostId(journalpostId)
            )
        }
        util.ventPåSvar(journalpostId.referanse, maxTid = 30)   // returns silently on timeout!

        val behandlinger = dataSource.transaction { connection ->
            postgresRepositoryRegistry.provider(connection).provide<BehandlingRepository>().hentAlleBehandlingerForJournalpost(journalpostId)
        }
        assertThat(behandlinger.map { it.typeBehandling }).containsExactlyInAnyOrder(
            TypeBehandling.Fordeling, TypeBehandling.Journalføring, TypeBehandling.DokumentHåndtering)
        assertThat(behandlinger).allSatisfy { assertThat(it.status()).isEqualTo(Status.AVSLUTTET) }
        // No KLAR or FEILET jobs left for the journalpost (FEILET would otherwise go unnoticed):
        assertThat(dataSource.transaction { FlytJobbRepository(it).hentJobberForSak(journalpostId.referanse) }).isEmpty()
    }
}
```

The database is shared by all tests in the class, so use new journalposts per test (or truncate tables in `@AfterEach` like `Flyttest`).

## 6. New Flyttest scenario (spans behandlinger, avklaringsbehov, vent or Kafka)

Add a method to `flyt/src/test/kotlin/no/nav/aap/postmottak/flyt/Flyttest.kt` instead of creating a new class: it already has Postgres, Kafka, a motor, a stream and helpers (`leggJournalpostPåKafka`, `triggProsesserBehandling`, `opprettJournalføringsBehandling`, `alleBehandlingerForJournalpost`, `hentBehandling`, `prøv`, and the `Behandling.løsAvklaringsBehov / sjekkÅpentAvklaringsbehov / verifiserErPåVent / verifiserIkkePåVent` extensions). Tables are truncated after each test.

```kotlin
@Test
fun `papirsøknad går gjennom tema, sak og digitalisering`() {
    val journalpostId = TestJournalposter.papirsøknad().journalpostId()
    leggJournalpostPåKafka { this.journalpostId = journalpostId.referanse }

    val journalføring = prøv { alleBehandlingerForJournalpost(journalpostId).first { it.typeBehandling == TypeBehandling.Journalføring } }!!
    util.ventPåSvar(journalpostId.referanse, journalføring.id.id)

    journalføring
        .sjekkÅpentAvklaringsbehov(Definisjon.AVKLAR_TEMA)
        .løsAvklaringsBehov(AvklarTemaLøsning(skalTilAap = true))     // solves via AvklaringsbehovHendelseHåndterer, then waits for jobs
        .løsAvklaringsBehov(AvklarSaksnummerLøsning(saksnummer = "123"))

    val dokument = prøv { alleBehandlingerForJournalpost(journalpostId).first { it.typeBehandling == TypeBehandling.DokumentHåndtering } }!!
    dokument.sjekkÅpentAvklaringsbehov(Definisjon.DIGITALISER_DOKUMENT)
}
```

Notes:
- `løsAvklaringsBehov` uses the behandling's current `versjon`; chain on its return value, which is the reloaded behandling.
- To change a feature toggle for one test, use the `TemaFlytUnleash` pattern (a delegating object with a `@Volatile` field) and reset it in `finally`.
- To simulate a change in Joark mid-test, call `TestJournalposter.leggTil { journalpostId = <same id>; ... }` again: it overwrites the fake.
- Run it alone first: `run-tests.sh flyt 'no.nav.aap.postmottak.flyt.Flyttest.<method name>'`, then read `flyt-trace.sh flyt`.

## 7. Mock-based tests (existing style, still acceptable)

Most step, løser and job tests construct the class directly with `mockk(relaxed = true)` collaborators (`AvklarSakStegTest`, `VurderRelevantDokumentForAAPJobbUtførerTest`). When extending those files keep their style. Guidelines:
- Prefer `every { repo.hent...() } returns value` plus assertions on returned results over long chains of `verify(exactly = n)`.
- Use `withArg { assertThat(...) }` inside `verify` to check stored values.
- Relaxed mocks return defaults (`false`, empty lists, `null` for nullable types), which can silently take a different branch; stub what the branch depends on.
- Call `clearAllMocks()` in `@AfterEach` when mocks are shared fields reused across tests with `every`.
