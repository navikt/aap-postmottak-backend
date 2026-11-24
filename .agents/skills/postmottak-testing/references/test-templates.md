# Testmaler

Mal 1 er kjørt grønt i november 2026, mal 2, 4, 5 og 6 i oktober 2026. Mal 3 er et skjelett med plassholdernavn. Mal 1 har alle importene. For de andre kopierer du importene fra mal 1 eller fra filene det henvises til.

## 1. Stegtest, fake-først (foretrukket for steglogikk)

Ekte stegwiring, ekte Postgres og ekte HTTP-klienter mot fakes. Malen er komplett med importer og kjørt grønt (november 2026). Legg filen ved siden av steget, for eksempel `flyt/src/test/kotlin/no/nav/aap/postmottak/forretningsflyt/steg/journalføring/`.

```kotlin
package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.FakeUnleash
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.modell.TestKelvinSak
import no.nav.aap.postmottak.test.modell.TestPersoner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Fakes
class AvklarSakStegMedFakesTest {
    private lateinit var dataSource: TestDataSource
    private val gatewayProvider = defaultGatewayProvider { register<FakeUnleash>() }

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource() // fersk database per test (klonet fra mal)
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `digital søknad knyttes automatisk til Kelvin-sak`() {
        val person = TestPersoner.leggTil { kelvinSak = TestKelvinSak(saksnummer = "S-123") }
        val journalpostId = TestJournalposter.leggTil { this.person = person; digitalSøknad() }.journalpostId()

        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            val behandlingId = repositoryProvider.provide<BehandlingRepository>()
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            val kontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)

            // Seed journalposten på samme måte som OPPDATER_FAKTAGRUNNLAG gjør (leser SAF-faken, upserter personen).
            val informasjonskrav = JournalpostInformasjonskrav.konstruer(repositoryProvider, gatewayProvider)
            val input = informasjonskrav.klargjør(kontekst)
            informasjonskrav.oppdater(input, informasjonskrav.hentData(input), kontekst)

            val resultat = AvklarSakSteg.konstruer(repositoryProvider, gatewayProvider).utfør(kontekst)

            assertThat(resultat).isEqualTo(Fullført)
            assertThat(repositoryProvider.provide<SaksnummerRepository>().hentSakVurdering(behandlingId)?.saksnummer)
                .isEqualTo("S-123")
        }
    }
}
```

For «trenger manuell behandling» asserter du `isInstanceOf(FantAvklaringsbehov::class.java)` og leser definisjonen med `(resultat.transisjon() as FunnetAvklaringsbehov).avklaringsbehov()`. For å teste ny kjøring etter en løsning lagrer du vurderingen (eller kjører løseren, mal 2) og kaller `utfør` på nytt.

## 2. Løsertest, fake-først

Samme oppsett som i mal 1, deretter:

```kotlin
AvklarSakLøser.konstruer(repositoryProvider, gatewayProvider).løs(
    AvklaringsbehovKontekst(Bruker("Z999999"), kontekst),
    AvklarSaksnummerLøsning(saksnummer = "S-1", journalposttittel = "Søknad om AAP"),
)
val vurdering = repositoryProvider.provide<SaksnummerRepository>().hentSakVurdering(behandlingId)
assertThat(vurdering?.saksnummer).isEqualTo("S-1")
```

Validering i løsere er stort sett `require(...)`: test avvist input med `assertThrows<IllegalArgumentException> { ... }` og assert at ingenting ble lagret.

## 3. Repository-test

Mønster fra `repository/src/test/.../BehandlingRepositoryImplTest.kt` og `SaksnummerRepositoryImplTest.kt`:

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

Dekk: lagre to ganger (bare den siste er aktiv), `kopier` og en behandling uten data (`null`).

## 4. Kafka-topologitest mot en ekte database

Kjører den ekte `JoarkKafkaHandler`-topologien med en `TopologyTestDriver` (ingen broker) og ekte repositorier. Den eksisterende `JoarkKafkaHandlerTest` gjør det samme med mockede repositorier.

```kotlin
@Fakes
class MinKafkaTest {
    private lateinit var dataSource: TestDataSource
    private val gatewayProvider = defaultGatewayProvider { register<FakeUnleash>() }
    private val config = StreamsConfig(
        applicationId = "postmottak-test",
        brokers = "localhost:9092", // blir aldri kontaktet, men TopologyTestDriver avviser tom verdi
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

        // Ren SQL: FlytJobbRepository.hentJobber* virker bare etter at en Motor har registrert jobbtypene.
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

Alle felt i `JournalfoeringHendelseRecord` uten standardverdi må settes før `build()`.

## 5. Motorjobbkjede (jobbene kjører for ekte, ingen Kafka)

Mønster fra `fordeler/arena/ArenaOppgaveFlytTest.kt`. Én motor og én database per klasse:

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
        util.ventPåSvar(journalpostId.referanse, maxTid = 30)   // returnerer stille ved timeout!

        val behandlinger = dataSource.transaction { connection ->
            postgresRepositoryRegistry.provider(connection).provide<BehandlingRepository>().hentAlleBehandlingerForJournalpost(journalpostId)
        }
        assertThat(behandlinger.map { it.typeBehandling }).containsExactlyInAnyOrder(
            TypeBehandling.Fordeling, TypeBehandling.Journalføring, TypeBehandling.DokumentHåndtering)
        assertThat(behandlinger).allSatisfy { assertThat(it.status()).isEqualTo(Status.AVSLUTTET) }
        // Ingen KLAR- eller FEILET-jobber igjen for journalposten (FEILET ville ellers gått ubemerket hen):
        assertThat(dataSource.transaction { FlytJobbRepository(it).hentJobberForSak(journalpostId.referanse) }).isEmpty()
    }
}
```

Databasen deles av alle testene i klassen, så bruk nye journalposter per test (eller tøm tabellene i `@AfterEach` slik `Flyttest` gjør).

## 6. Nytt Flyttest-scenario (går på tvers av behandlinger, avklaringsbehov, vent eller Kafka)

Legg til en metode i `flyt/src/test/kotlin/no/nav/aap/postmottak/flyt/Flyttest.kt` i stedet for å lage en ny klasse: den har allerede Postgres, Kafka, en motor, en stream og hjelpere (`leggJournalpostPåKafka`, `triggProsesserBehandling`, `opprettJournalføringsBehandling`, `alleBehandlingerForJournalpost`, `hentBehandling`, `prøv`, og extension-funksjonene `Behandling.løsAvklaringsBehov / sjekkÅpentAvklaringsbehov / verifiserErPåVent / verifiserIkkePåVent`). Tabellene tømmes etter hver test.

```kotlin
@Test
fun `papirsøknad går gjennom tema, sak og digitalisering`() {
    val journalpostId = TestJournalposter.papirsøknad().journalpostId()
    leggJournalpostPåKafka { this.journalpostId = journalpostId.referanse }

    val journalføring = prøv { alleBehandlingerForJournalpost(journalpostId).first { it.typeBehandling == TypeBehandling.Journalføring } }!!
    util.ventPåSvar(journalpostId.referanse, journalføring.id.id)

    journalføring
        .sjekkÅpentAvklaringsbehov(Definisjon.AVKLAR_TEMA)
        .løsAvklaringsBehov(AvklarTemaLøsning(skalTilAap = true))     // løser via AvklaringsbehovHendelseHåndterer, venter deretter på jobbene
        .løsAvklaringsBehov(AvklarSaksnummerLøsning(saksnummer = "123"))

    val dokument = prøv { alleBehandlingerForJournalpost(journalpostId).first { it.typeBehandling == TypeBehandling.DokumentHåndtering } }!!
    dokument.sjekkÅpentAvklaringsbehov(Definisjon.DIGITALISER_DOKUMENT)
}
```

Merknader:
- `løsAvklaringsBehov` bruker behandlingens gjeldende `versjon`. Lenk på returverdien, som er den lastede behandlingen på nytt.
- For å endre en feature toggle i én test bruker du mønsteret `TemaFlytUnleash` (et delegerende objekt med et `@Volatile`-felt) og tilbakestiller det i `finally`.
- For å simulere en endring i Joark midt i testen kaller du `TestJournalposter.leggTil { journalpostId = <samme id>; ... }` på nytt: det overskriver faken.
- Kjør den alene først: `run-tests.sh flyt 'no.nav.aap.postmottak.flyt.Flyttest.<metodenavn>'`, og les deretter `flyt-trace.sh flyt`.

## 7. Mock-baserte tester (eksisterende stil, fortsatt akseptabelt)

De fleste steg-, løser- og jobbtester konstruerer klassen direkte med `mockk(relaxed = true)`-samarbeidspartnere (`AvklarSakStegTest`, `VurderRelevantDokumentForAAPJobbUtførerTest`). Når du utvider disse filene, behold stilen deres. Retningslinjer:
- Foretrekk `every { repo.hent...() } returns value` pluss asserts på returnerte resultater framfor lange kjeder av `verify(exactly = n)`.
- Bruk `withArg { assertThat(...) }` inni `verify` for å sjekke lagrede verdier.
- Relaxed mocks returnerer standardverdier (`false`, tomme lister, `null` for nullbare typer), som i det stille kan ta en annen gren. Stubb det grenen avhenger av.
- Kall `clearAllMocks()` i `@AfterEach` når mocks er delte felt som gjenbrukes på tvers av tester med `every`.
