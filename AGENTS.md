# aap-postmottak-backend – instruksjoner for KI-agenter

Postmottak for arbeidsavklaringspenger (Kelvin): tar imot journalposter på tema AAP fra Joark, avgjør om de skal
til Kelvin eller Arena, journalfører dem og overleverer innholdet til behandlingsflyt. Kotlin 2.4, Ktor, Postgres,
Kafka (bare inn), Gradle. Kopiert fra aap-behandlingsflyt i august 2024; begrepene er de samme, koden er sin egen.
Les dette før du rører et steg, en løser, en fordelingsregel, en jobb, en klient eller et repository.

## Språk og stil
- Norsk bokmål overalt: identifikatorer (med æøå), KDoc, loggmeldinger, testnavn (backtick-setninger),
  commit-meldinger. Ikke translitterer (`loes`), ikke bland inn engelsk.
- Kotlin official style, 4 mellomrom, 120 kolonner, ingen trailing comma, ingen star imports (`.editorconfig`).
  Ingen formatter i CI; følg filen du står i.
- Feilhåndtering: `UgyldigForespørselException` for brukerfeil (HTTP 400), `error("…")`/`requireNotNull` for
  invarianter. Ikke innfør `Result`-typer eller nye unntakshierarkier.
- Blokkerende IO uten coroutines. Transaksjoner åpnes i `api` (per kall), av Motor (per jobb) og i Kafka-mottaket
  (`TransactionProvider`); domenet ellers åpner aldri transaksjoner.

## Moduler
| Modul | Innhold |
|---|---|
| `app` | Oppstart. `App.kt` er komposisjonsroten: Flyway, Motor med `ProsesseringsJobber.alle()`, Kafka-strømmen, API-modulene. |
| `api` | Ktor/OpenAPI-ruter (`flyt/`, `faktagrunnlag/`, `drift/`, `auditlog/`, `test/`) og DTO-er. Ingen logikk i rutefiler, ingen tester i dag. |
| `flyt` | Domene og motor. Fire pakkerøtter: `no.nav.aap.postmottak` (flyt, steg, avklaringsbehov, faktagrunnlag, gateway-grensesnitt, mottak, prosessering), `no.nav.aap.fordeler` (regler, Arena-veien), `no.nav.aap.lookup` (repository-grensesnitt), `no.nav.aap.unleash` (toggles). |
| `klienter` | HTTP-klienter (SAF, PDL, Gosys, NORG, NOM, EREG, arenaoppslag, behandlingsflyt, oppgave, statistikk …) og `DefaultGatewayProvider`. Unntak: skriving til Joark ligger i `flyt/…/gateway/JournalføringService.kt`. |
| `repository` | `*RepositoryImpl` (håndskrevet SQL via dbconnect), `PostgresRepositoryRegistry`, Flyway i `src/main/resources/flyway/`. |
| `lib-test` | `FakeServers` og fakes for alle eksterne systemer, `FakeUnleash`, `TestPersoner`/`TestJournalPost`, `@Fakes`. |
| `kontrakt` | **Publiseres som bibliotek** `no.nav.aap.postmottak:kontrakt`: `Definisjon`, `AvklaringsbehovKode`, `StegType`, `TypeBehandling`, `Status`, `DokumentflytStoppetHendelse`. |
| `arkitektur` | Bare tester: Konsist-regler for registreringslister, lagdeling og `aktiv`-flagg. |

Grensesnitt for repositories og gateways ligger i `flyt` ved siden av modellen; implementasjonene i `repository` og
`klienter`. `GjenopptakRepositoryImpl` ligger feilaktig i `flyt`, og tre `*RepositoryImplTest` ligger i `flyt/src/test`;
ikke bruk dem som mal.

## Kommandoer
```bash
./gradlew :flyt:compileKotlin                              # rask kompilering av én modul
./gradlew coreTest                                         # kjernetestene i alle moduler, uten Docker (~15 s varm)
./gradlew :flyt:coreTest                                   # kjernetestene i én modul
./gradlew :flyt:test --tests "*AvklarTemaStegTest"         # én testklasse (utagget klasse = ingen Docker)
./gradlew :repository:test --tests "*BehandlingRepositoryImplTest"  # DB-test, krever Docker (Testcontainers)
./gradlew :flyt:test --tests "*Flyttest"                   # ende-til-ende med Postgres og Kafka, krever Docker (~30 s)
./gradlew :arkitektur:test                                 # strukturregler: registreringslister, lagdeling, aktiv-flagg
./gradlew test                                             # alt, som CI (~1,5 min lokalt), krever Docker
./gradlew :app:runTestApp                                  # app på :8070 med fakes og Testcontainers-Postgres (krever Docker)
./gradlew :app:genererOpenApiJson                          # oppdaterer openapi.json, krever Docker (frontend-typer genereres fra den)
```
Testklasser som trenger Docker er tagget `@Tag("db")` (Postgres) eller `@Tag("kafka")` (bare `Flyttest`); `coreTest`
kjører resten. En ny test som bruker `TestDataSource`, `postgreSQLContainer()` eller Kafka uten tag feiler i `coreTest`
med beskjed om hvilken tag som mangler. `coreTest` er et alias: det tar ikke `--tests` og kan ikke stå i samme kall som
`test`/`check`/`build`. Bruk `:modul:test --tests …` for én klasse. Agentens sandkasse har normalt ikke Docker: da er
`coreTest` og `:arkitektur:test` sjekken, og DB-/Kafka-testene kjøres av mennesket eller CI. Si fra hva som ikke er kjørt.
Feiler Testcontainers med Ryuk, «lease does not exist» eller «Connection refused», er det Docker-miljøet, ikke koden.
Ikke bruk `assemble`/`build` i arbeidsløkka. `./gradlew detekt` feiler aldri bygget; ikke rydd gamle funn i urelaterte PR-er.
Kjør git uten pager og editor (`git --no-pager log`, `git --no-pager diff`, `git merge --no-edit`).

## Harde regler (brudd knekker prod, historikk eller andre team)
1. **Aldri ekte persondata.** Ingen ekte fødselsnummer, aktør-id, navn eller dokumentinnhold i prompt, kode, logg,
   testdata eller commit. Bruk `genererIdent(…)`/`TestPersoner.leggTil { … }` og `TestJournalPost` fra `lib-test`.
   Logg aldri `Ident.identifikator`, `Person`-objekter eller strukturert dokumentinnhold; `journalpostId` og
   `behandlingId` er greit. Ikke kjør RUNBOOK-spørringene eller lim inn resultat fra dem.
2. **Aldri endre, slette eller omdøpe lagrede navn.** Verdier i `Definisjon`, `AvklaringsbehovKode`, `StegType`,
   `TypeBehandling` og `Status` (kontrakt), jobbenes `type`-streng, enumer som lagres som tekst
   (`InnkommendeJournalpostStatus`, `ÅrsakTilStatus`, `AapSystem`, `Tema`, `ÅrsakTilSettPåVent` …), regelklassenes
   navn (`regelNavn()` lagres i `REGEL_EVALUERING`) og `PostmottakFeature`-navn (er Unleash-nøkkelen). Legg til ny
   verdi og merk den gamle `@Deprecated` (se `InnkommendeJournalpostStatus`).
3. **Alt under `kontrakt/` publiseres automatisk** som ny versjon ved merge til main og brukes av aap-oppgave,
   aap-statistikk og aap-tilgang. Bare legg til. `AvklaringsbehovKode` (konstant og enum) og `Definisjon` endres
   samlet. Si fra før du rører `kontrakt/`. DTO-er i `api/` er også kontrakt: frontendene genererer typer fra OpenAPI.
4. **Flyway-filer er uforanderlige.** Ny fil `V1.<neste>__beskrivelse.sql`; migrasjonen må tåle at gamle poder kjører
   mot nytt skjema under rullende deploy (nullable/`DEFAULT`, ingen `DROP`/`RENAME` sammen med koden som slutter å bruke det).
5. **Grunnlag oppdateres aldri in-place.** Tabeller med `aktiv`-kolonne skrives med «deaktiver + insert ny rad»;
   `UPDATE` på dem er bare lov for å sette `aktiv = false` (`:arkitektur:test` sjekker det).
6. **Git**: aldri push til main, alltid PR. Ikke commit eller push uten beskjed. Main går i prod uten manuell port.

## Flytmodellen – det ikke-opplagte
- **Tre flyter i kjede**, én behandling per flyt (`TypeBehandling` → flytobjekt i `forretningsflyt/behandlingstyper/`,
  koblet i `flyt/…/postmottak/flyt/BehandlingType.kt`). Alle starter med `StartBehandlingSteg` og slutter med
  `AvsluttBehandlingSteg`:
  - `Fordeling` → `Fordelingflyt`: `AvklarFordelingSteg` (Kelvin eller Arena?) → `FordelingVideresendSteg`.
  - `Journalføring` → `Journalføringsflyt`: `AvklarTemaSteg` → `AvklarSakSteg` → `SettFagsakSteg` →
    `JournalføringSteg` (ferdigstill i Joark) → `VideresendSteg`.
  - `DokumentHåndtering` → `Dokumentflyt`: `DigitaliserDokumentSteg` (dokumentet fra SAF blir en strukturert
    `Melding`, ellers avklaringsbehov) → `OverleverTilFagsystemSteg` (sender meldingen til behandlingsflyt).
  Nest siste steg oppretter neste behandling: `FordelingVideresendSteg` bare ved KELVIN (ARENA går til Arena-jobbene),
  `VideresendSteg` alltid, og den kopierer grunnlaget til `DokumentHåndtering` med `GrunnlagKopierer`. Et nytt
  grunnlag som skal følge med, må derfor implementere `kopier()`.
- **Inn kommer alt** med tema AAP og status MOTTATT fra topic `teamdokumenthandtering.aapen-dok-journalfoering`
  (`mottak/KafkaStreams.kt`, EESSI filtreres bort). `VurderRelevantDokumentForAAPJobbUtfører` lager
  `innkommende_journalpost` og første behandling (`Fordeling`). Brevkoden (`Brevkoder.kt`) styrer resten, ikke mottaket.
- **Fordelingsreglene** (`fordeler/regler/`) er `sealed`: en ny regel i pakken blir funnet automatisk via
  `RegelFactory::class.sealedSubclasses` og slås på per miljø med `erAktiv = miljøConfig(prod, dev)`.
  `Regelresultat.skalTilKelvin()` bestemmer: overstyrt til Arena vinner, Kelvin-sak (ikke anke/reisestønad) gir Kelvin,
  ellers må alle øvrige regler være sanne. En regel svarer altså `true` = «kan gå til Kelvin». Reglene i dag:
  `Aldersregel`, `SøknadRegel`, `ErIkkeAnkeRegel`, `ErIkkeReisestønadRegel`, `ArenaHistorikkRegel`, `KelvinSakRegel`,
  `ArenaSakRegel`, `ManueltOverstyrtTilArenaRegel`. Resultatet caches i `innkommende_journalpost`.
- **Hvert steg kjøres mange ganger**: hver `ProsesserBehandling`-jobb starter fra aktivt steg, og tilbakeføring kjører
  steg om igjen. `utfør` må være idempotent: enten «vurdering finnes → `Fullført`» først i steget
  (`AvklarFordelingSteg`, `DigitaliserDokumentSteg`), eller avstemming via `AvklaringsbehovService` (`AvklarTemaSteg`).
- **Ett steg = fasene** START → OPPDATER_FAKTAGRUNNLAG → UTFØRER → AVKLARINGSPUNKT → AVSLUTTER, alle i jobbens ene
  transaksjon under skrivelås (`TaSkriveLåsRepository.lås`/`verifiserSkrivelås`, optimistisk på `behandling.versjon`).
- **Avklaringsbehov**: steget returnerer `FantAvklaringsbehov(Definisjon.X)` eller bruker
  `AvklaringsbehovService.oppdaterAvklaringsbehov(…)` for behov som kan reises og avbrytes. Et løst behov
  (`POST /api/behandling/løs-behov`) legger en ny `ProsesserBehandling`-jobb; flyten går videre asynkront (HTTP 202).
- **Ventepunkt** (`MANUELT_SATT_PÅ_VENT`, `VENT_PA_GOSYS`) har frist; `GjenopptaBehandlingJobbUtfører` tar dem av vent
  hver morgen, og Kafka-hendelser kan ta `VENT_PA_GOSYS` av vent før det.
- **Arena-veien** er jobber i `fordeler/arena/jobber/` (de som sender til Arena arver `ArenaJobbutførerBase`), ikke steg.
  **Kelvin-veien** ender i `BehandlingsflytGateway.sendHendelse`. Postmottak har ingen Kafka-produsent; oppgave og
  statistikk får beskjed over REST fra `StoppetHendelseJobbUtfører`.
- **Informasjonskrav** (bare `JournalpostInformasjonskrav` og `SaksnummerInfoKrav`) kjører før steget og kan
  tilbakeføre flyten ved `ENDRET`.

## Registreringslister som må oppdateres for hånd
Kompilerer uten endring og feiler først i runtime (`IllegalStateException` «… er ikke registrert» eller
`NoSuchElementException` når Motor plukker jobben):
`repository/…/PostgresRepositoryRegistry.kt`, `klienter/…/DefaultGatewayProvider.kt`,
`flyt/…/prosessering/ProsesseringsJobber.kt`, og nye steg i en flytdefinisjon under `forretningsflyt/behandlingstyper/`.
`./gradlew :arkitektur:test` sjekker disse og sier hvilken fil som mangler hva. Kjente brudd står i `kjenteBrudd` i
testene; listene skal bare krympe, så ikke legg til nye for å få testen grønn.
Ny toggle: `PostmottakFeature` + begge `when` i `lib-test/…/FakeUnleash.kt` (kompilatoren sier fra; fakens verdi er
det testene kjører med). Ny klient: også `.nais/dev-gcp.yaml` og `.nais/prod-gcp.yaml` (URL, scope, `outbound`) og en
fake i `lib-test/…/FakeServers.kt`. Regler, løsninger (`AvklaringsbehovLøsning` er `sealed`) og visningsutledere finnes
via `sealedSubclasses` og trenger ingen liste.
Alle injiserbare klasser har en `companion object` som konstruerer dem fra providerne: `FlytSteg`, `LøserKonstruktør`,
`Informasjonskravkonstruktør`, `ProvidersJobbSpesifikasjon`, `RegelFactory`, `Factory` (repository og klient).
Ingen DI-ramme. Parametertyper er grensesnittet, aldri `*Impl`.

## Testing
- Nivåer: stegtester, løsertester, regeltester og jobbtester med mockk uten DB (`flyt/src/test`, kjører i `coreTest`);
  klienttester mot fake-servere med `@Fakes`; `*RepositoryImplTest` mot Postgres (`TestDataSource`, `@Tag("db")`);
  `Flyttest` ende-til-ende fra Kafka-melding til ferdig behandling (`@Tag("db")` + `@Tag("kafka")`); `ApiTest` i `app`.
- Nytt steg → `XxxStegTest` og et scenario i `Flyttest` hvis flyten endres. Ny løser → `XxxLøserTest`. Ny regel →
  regeltest + `RegelResultatTest`. Nytt repository → `XxxRepositoryImplTest` i `repository/src/test`. Ny klient → test
  mot faken. Ny toggle → test med toggle av og på (`FakeUnleash` gir én verdi; overstyr med mockk i stegtesten).
- Fakes og syntetiske personer framfor ekte data. Ikke «fiks» en test ved å endre forventningen uten å forstå hvorfor
  den feilet.
- Feilutsatte områder (andel fiks-commits 1,4–1,5 × snittet): `fordeler/arena`, `fordeler/regler`, `klient/pdl`,
  `klient/norg`, `klient/gosysoppgave`, `JournalpostService.kt`. Her skal det alltid være test for den nye grenen.
- Før du sier «ferdig»: kompiler endrede moduler, kjør `coreTest` og `:arkitektur:test`, og si hvilke DB-/Kafka-tester
  som gjenstår for mennesket eller CI.

## Naborepoer
Les kildekoden i naborepoet i stedet for å pakke ut jar-filer. Finnes ikke mappen, si fra i stedet for å lete i
Gradle-cachen. Endringer i et naborepo er en egen PR der; ikke skriv der uten at mennesket ber om det.
- `../aap-behandlingsflyt`: `no.nav.aap.behandlingsflyt:kontrakt` (hendelse-DTO-ene til `BehandlingsflytGateway`,
  `statistikk`, `sak`) ligger under `kontrakt/`. Versjonen vi bruker står som `behandlingsflyt` i
  `gradle/libs.versions.toml` og er en git-tag der: `git -C ../aap-behandlingsflyt show <versjon>:kontrakt/<sti>`.

## Dokumentasjon
- `README.md` (lokal kjøring, tester), `RUNBOOK.md` (driftsspørringer; inneholder SQL som henter fødselsnummer, bare for mennesker).
- Swagger i dev: https://aap-postmottak-backend.intern.dev.nav.no/swagger-ui/index.html
- Testendepunkter i `api/…/test/` ligger bevisst i `main` (skrudd av i prod). Ikke slett dem, ikke legg prodkode der.
