---
name: postmottak-testing
description: Hvordan skrive, kjøre og feilsøke tester i aap-postmottak-backend - fakes framfor mocks (lib-test FakeServers, TestJournalposter, TestPersoner, FakeUnleash, TestDataSource), hvilken testtype du velger (ren, steg/løser, repository, klient, Kafka-topologi, motorjobb, Flyttest), hvordan du kjører smale testsett uten sandbox-timeouts, og hvordan du leser store flyt-testutdata billig med de medfølgende skriptene. Bruk før du skriver, endrer, kjører eller feilsøker en test.
---

# Testing i postmottak

Hvis brukeren eller koden motsier denne skillen, varsle brukeren og spør om skillen og/eller `AGENTS.md` skal oppdateres. Se `AGENTS.md` → "When the user contradicts this documentation".

## Prinsipper

1. **Unngå mocking. Foretrekk repositorier i minnet og fakes.** En fake er en fungerende implementasjon med styrbar tilstand:
   - Eksterne HTTP-systemer: annoter klassen med `@Fakes` (starter lokale fake-servere, se `references/fakes.md`) og bruk de ekte klientene via `defaultGatewayProvider { register<FakeUnleash>() }`. Styr svarene deres med `TestJournalposter` og `TestPersoner`.
   - Gatewayer på grensesnittnivå: små fakes i koden, registrert med `createGatewayProvider { register<FakeX>() }` (`FakeUnleash`, `FakeArenaoppslagGateway`, `FakeStatistikkGateway`, ...). Å registrere en implementasjon erstatter alle tidligere implementasjoner av samme grensesnitt.
   - Database: ekte Postgres gjennom `TestDataSource` (komponenter dbtest; hver instans er en fersk database klonet fra en migrert mal) og `postgresRepositoryRegistry.provider(connection)`. Repositorier i minnet i `lib-test/.../fakes` (`InMemoryAvklaringsbehovRepository`, `InMemoryUnder18Repository`) finnes for ren motorlogikk og for repositorier som ennå ikke har en Postgres-implementasjon.
2. **Bruk mock bare når oppførselen ikke med rimelighet kan testes med en fake**, for eksempel for å framkalle en feil som er vanskelig å produsere. Ikke bruk mocks bare for å isolere samarbeidspartnere. Asserter på lagret tilstand og resultater framfor antall kall. Eldre mock-baserte tester kan bli stående, men bruk fakes også når du utvider dem.
3. **Gå gjennom den ekte wiringen.** Bygg steg med `MittSteg.konstruer(repositoryProvider, gatewayProvider)` og løsere med `MinLøser.konstruer(...)`, slik at manglende registreringer og feil oppslag feiler i testene.
4. **Velg den billigste testtypen som beviser oppførselen** (tabellen under). Legg til et `Flyttest`-scenario bare for oppførsel som går på tvers av steg, jobber, behandlinger eller Kafka.
5. **Hold testingen proporsjonal for hjelpeverktøy som bare brukes i dev.** Endepunkter som bare brukes til manuell testing i dev eller lokalt, trenger ikke full automatisk dekning. Bruk som utgangspunkt en kompileringssjekk og manuell verifisering i Swagger. Ikke legg til egne endepunkttester eller omfattende dekning av autentisering, validering og feiltilfeller med mindre det finnes betydelig egen logikk eller brukeren ber om det. Behold miljøbegrensninger og autentisering. Unntaket reduserer ikke testingen av delte produksjonsklienter, gatewayer eller forretningslogikk.
6. **Test klientens kontrakt, ikke RestClient selv.** Fokuser på HTTP-metode, sti, payload, mapping av svar og klientspesifikk validering eller feilhåndtering. Ikke dupliser RestClients generelle håndtering av HTTP-statuskoder, headere, tokens eller JSON-deserialisering. Test slik oppførsel bare når klienten konfigurerer eller overstyrer den.
   Ikke list opp varianter med manglende, null og feil type for vanlige DTO-felt bare for å utøve Jackson. Bruk typede fixtures til å teste eksplisitt klientvalidering, og unngå å gjenta samme validering gjennom JSON-varianter.
7. **Test egen logikk, ikke transaksjonsgarantier.** For informasjonskrav: test at både `ENDRET` og `IKKE_ENDRET` registreres med input uten registerdata, at siste kjøring finnes på tvers av behandlinger, og at avhengigheter er oppdatert før neste `klargjør`. Ikke legg til en egen test for at en feilet oppdatering ikke lagrer vellykket kjøring; dette håndteres av transaksjonen.
8. **Navngi tester etter det de sjekker.** Beskriv scenarioet og forventet resultat, ikke handlinger testen ikke utfører.

## Testtyper

| Type | Beviser | Infrastruktur | Eksempel |
| --- | --- | --- | --- |
| Ren enhetstest | regler, parsing, domenelogikk | ingen | `RegelResultatTest`, `JournalpostTest`, `SaksinfoTest` |
| Steg, løser eller tjeneste, fake-først | beslutninger med ekte database og HTTP-wiring | `TestDataSource` + `@Fakes` | `references/test-templates.md`, mal 1; `under18/Under18JournalposterInformasjonskravTest` |
| Steg, mock-basert (eldre tester) | beslutningstabeller isolert | mockk | `AvklarSakStegTest`, `AvklarFordelingStegTest` |
| Repository | SQL, grunnlag/aktiv, `kopier` | `TestDataSource` | `repository/src/test/...`, `SaksnummerRepositoryImplTest` |
| Klient | HTTP-klienter mot fakes | `@Fakes` | `klienter/src/test/...` |
| Kafka-topologi | filtrering og forgrening i `JoarkKafkaHandler` | `TopologyTestDriver`, ingen broker | `JoarkKafkaHandlerTest` |
| Motorjobbkjede | jobbsekvenser, fallbacks | `TestDataSource` + `Motor` + `@Fakes` | `fordeler/arena/ArenaOppgaveFlytTest` |
| Flyt, ende til ende i samme prosess | forløp på tvers av behandlinger, avklaringsbehov, vent og Kafka | Postgres- og Kafka-containere + `Motor` + `@Fakes` | `flyt/Flyttest` |

Maler for hver type: `references/test-templates.md`. Hva hver fake returnerer: `references/fakes.md`.

## Kjøre tester uten timeouts

Forutsetninger: Docker for Testcontainers (Postgres 18, Kafka `apache/kafka-native` for `Flyttest`). Med Colima eksporterer du de tre variablene `TESTCONTAINERS_*`/`DOCKER_HOST` fra README. Gradle bruker en JDK 25-toolchain.

- **Kjør alltid hele suiten med `./gradlew test` før du sier at en oppgave er ferdig.** Kjør den én gang til slutt, ikke mens du itererer. Smale kjøringer overser andre testklasser som konstruerer de samme stegene eller løserne. Eksempel: en ny gateway-avhengighet i en løser ødela `AvklaringsbehovsLøserTest`, som bygger hver løser med sin egen `createGatewayProvider`. Suiten kjører alle moduler parallelt (`org.gradle.parallel=true`), og testene i hver modul kjører i `cores / 2` JVM-forks som hver starter sine egne containere. Regn med flere minutter, og bruk lang timeout eller kjør i bakgrunnen. Tester i uendrede moduler kan komme fra Gradles build-cache. Det er greit.
- Mens du itererer, bruk det medfølgende skriptet. Det bruker Gradles up-to-date-sjekk og build-cache (en cachet task viser null tester og `UP-TO-DATE` eller `FROM-CACHE`; det betyr at ingenting har endret seg siden forrige vellykkede kjøring, så ikke tving `--rerun`), begrenser til `--max-workers=2`, lagrer hele loggen i en midlertidig fil og skriver ut en kort oppsummering med feilmeldinger:
  ```bash
  .agents/skills/postmottak-testing/scripts/run-tests.sh flyt 'no.nav.aap.postmottak.forretningsflyt.steg.journalføring.AvklarSakStegTest'
  .agents/skills/postmottak-testing/scripts/run-tests.sh flyt 'no.nav.aap.postmottak.flyt.Flyttest.kjører en manuell søknad igjennom hele flyten'
  .agents/skills/postmottak-testing/scripts/run-tests.sh repository '*JournalpostRepositoryImplTest'
  ```
  Filtre: fullt kvalifisert klasse, klasse + metodenavn (mellomrom er tillatt, ingen parametere for parametriserte tester) eller `*Suffiks`.
- **Flyt-tester: én metode om gangen mens du itererer**, hele klassen når du er ferdig. Målt på en bærbar PC: en enkelt `Flyttest`-metode ≈ 15 s klokketid, hele klassen (24 tester) ≈ 40 s, en fake-først steg-testklasse ≈ 6–9 s. Start aldri flere flyt-testklasser eller moduler parallelt.
- Bare kompilering: `./gradlew :flyt:compileTestKotlin`. Lint: `./gradlew detekt` (ikke en del av `check`).
- Hvor testene ligger: motor, steg, løsere, jobber og Kafka i `flyt`, repository-implementasjoner i `repository` (noen få i `flyt`), klienter i `klienter`, app og API i `app`, og `kontrakt` har én liten test.
- `<module>/build/test-results/test/` inneholder bare klassene fra den **siste** kjøringen.
- `ApiTest` i `app` skriver `openapi.json` i repo-roten (gitignorert) som en bieffekt.

## Lese utdata billig

Rapporter fra flyt-tester er enorme (`Flyttest`-XML ≈ 1 MB, én stdout-blokk per klasse). Ikke åpne dem. Gjør i stedet dette:

```bash
.agents/skills/postmottak-testing/scripts/flyt-trace.sh flyt                 # motorspor fra siste :flyt:test-kjøring
.agents/skills/postmottak-testing/scripts/flyt-trace.sh flyt '\[b=2\]'       # bare behandlingId 2
```

Skriptet beholder jobb-plukking/-ferdigstilling/-opprettelse, steg, resultater, avklaringsbehov, tilbakerullinger, advarsler, feil og exception-linjer (omtrent 10 ganger mindre). Semantikken bak motorlinjene står i skillen `postmottak-flyt-motor`.

## Fallgruver i dette repoet

1. `TestUtil.ventPåSvar` **feiler aldri**: etter `maxTid` logger den «Avbryter venting nå» og returnerer, og den regner `FEILET` jobber som ferdige. Hvis en flyt-test ser en halvprosessert behandling, se først etter `Feil under prosessering av jobb` i sporet.
2. Fake-tilstand er global og blir aldri tømt: `TestJournalposter`, `TestPersoner` og `FssOppgaver` er JVM-singletons. Opprett alltid nye data gjennom byggerne (tilfeldige id-er), og assert aldri på globale antall.
   Lag aldri tilfeldige fødselsnumre (`Random.nextLong(...)`) for personer eller identer. Bruk hjelperne i `lib-test/.../test/modell/TestPerson.kt`:
   - `TestPersoner.leggTil { }` når en fake-server må kjenne personen (SAF, PDL, NOM, behandlingsflyt, Arena).
   - `TestPerson()` og `person.aktivIdent()` når testen bare trenger identer, for eksempel i repository-tester. Legg til en historisk ident med `TestPerson().let { it.copy(identer = it.identer + Ident(genererIdent(dato).identifikator, aktivIdent = false)) }`.
   - `genererIdent(fødselsdato)` for ett gyldig fnr med en gitt fødselsdato.
3. Opprett journalposter gjennom `TestJournalposter.leggTil { }`: den registrerer personen, og NOM-faken svarer 404 for ukjente personer. Standardverdier i byggeren: digital **legeerklæring**, kanal NAV_NO, tema AAP, status MOTTATT, ny person, ingen avsenderMottaker.
4. `defaultGatewayProvider()` **uten** `register<FakeUnleash>()` bruker den ekte Unleash-klienten mot HTTP-faken, som melder **alle** toggles som påslått. `FakeUnleash` har derimot `PostmottakVelgTema` av og kaster exception for `DummyFeature`. Toggle-overstyringer i tester er delte objekter. Tilbakestill dem i `finally` (se `TemaFlytUnleash` i `Flyttest`).
5. Testene kjører som «dev»: `Aldersregel` og `SøknadRegel` er inaktive, så fordelingen er annerledes enn i prod.
6. `PrometheusProvider.prometheus` er en global `lateinit`. `@Fakes` og `WithDependencies` setter den. Rene enhetstester som konstruerer klienter eller enkelte steg, må sette den selv.
7. Hver `@Fakes`-klasse starter et nytt sett med omtrent 16 fake-servere og peker systemegenskapene om. De stoppes aldri med vilje: klienter er late singletons som beholder URL-en de først leste.
8. AssertJ `anySatisfy { }` / `allSatisfy { }` tar consumers. En lambda som bare beregner en Boolean, asserter ingenting (den siste asserten i `Flyttest."Blir satt på vent for etterspørring av informasjon"` er slik). Bruk `assertThat` inni, eller `anyMatch`.
9. Repositorier i minnet er globale objekter og beholder tilstanden gjennom hele JVM-ens levetid. Ikke tøm tilstanden mellom testene. Gi heller hver test egne id-er, slik at testene ikke kan se hverandres data: bygg journalposter med `TestJournalposter.leggTil { }` (tilfeldig `journalpostId`), og bruk id-er som ikke gjenbrukes mellom tester. `Flyttest` tømmer tabellene uten å starte identitetssekvensene på nytt, så `behandlingId`-ene forblir unike på tvers av tester.
10. Ikke kjør en filskriving og en Gradle-test i samme parallelle batch av verktøykall. Gradle kan starte før filen finnes.
11. Dokumentflyt har ikke informasjonskrav: en `DokumentHåndtering`-behandling som opprettes direkte i en test, feiler i DIGITALISER_DOKUMENT med mindre en journalpost er lagret for journalpostId først (kjør `JournalpostInformasjonskrav` eller en Journalføring-behandling).
12. Fake-datoer: SAF bruker journalpostens `mottattTid` som `DATO_REGISTRERT`. Standard er 2020-12-01 kl. 10:00 og er stabil ved gjentatte oppslag. Sett `mottattTid` på `TestJournalposter.leggTil { }` når datoen har betydning for testen.
13. `FlytJobbRepository.hentJobberForSak/ForBehandling` parser jobbtyper gjennom motorens globale `JobbType`-register, som bare fylles når en `Motor` konstrueres. I en klasse uten motor feiler dette med `Key <type> is missing in the map`, eller det går bra hvis en annen klasse kjørte først i samme JVM. Hent jobbene fra `JOBB` med SQL der (mal 4).
14. `TopologyTestDriver` avviser tom `bootstrap.servers`: gi `StreamsConfig(brokers = "localhost:9092")` en dummyverdi.

## Referanser

- `references/fakes.md`: hver fake-server og fake i koden, hva den returnerer og hvordan du styrer den.
- `references/test-templates.md`: kopierbare maler for hver testtype, inkludert hvordan du løser avklaringsbehov og sender Kafka-records i flyt-tester.
- `scripts/run-tests.sh`, `scripts/flyt-trace.sh`: kjør smale testsett og trekk ut et kompakt motorspor.
