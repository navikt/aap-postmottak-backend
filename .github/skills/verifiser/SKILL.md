---
name: verifiser
description: Kjør riktig sett med kompilering og tester for filene som er endret, og rapporter resultatet ærlig. Bruk før du sier at noe er ferdig, og alltid før en PR. Tar valgfritt en basis-ref (for eksempel origin/main) for en ren arbeidskopi.
argument-hint: "[basis-ref, for eksempel origin/main]"
---
# Verifiser endringene

1. Finn endrede filer. Bruk alltid `git -c core.quotePath=false`, ellers blir stier med æøå (`journalføring/`,
   `løser/`, `lås/`) sitert og ødelagt. Er arbeidskopien skitten (`git status --porcelain` gir noe), bruk
   `git -c core.quotePath=false status --porcelain -uall | awk '{print $NF}'`. Er den ren, bruk
   `git -c core.quotePath=false diff --name-only <basis>...HEAD`, der basis er det brukeren skrev etter `/verifiser`
   (for eksempel `/verifiser origin/main`). Finnes ingen basis, si det og stopp. Gradle-moduler er første
   stikomponent: `| cut -d/ -f1 | sort -u | grep -E '^(api|app|flyt|klienter|kontrakt|lib-test|repository|arkitektur)$'`.
2. Kompiler hver endret modul: `./gradlew :<modul>:compileKotlin :<modul>:compileTestKotlin`.
3. Kjør kjernetestene for hver endret modul: `./gradlew :<modul>:coreTest` (uten Docker). Har du endret `lib-test`,
   `build-logic`, `gradle/` eller `settings.gradle.kts`, kjør `./gradlew coreTest` for alle moduler.
   Feiler den fordi en test prøver å bruke Docker, mangler testklassen `@Tag("db")`/`@Tag("kafka")`: legg den til,
   ikke slett testen. Kjør så `./gradlew :arkitektur:test` i et eget kall (`coreTest` og `test` kan ikke stå i samme
   Gradle-kall). Feiler den, rett koden etter meldingen; ikke legg nye brudd inn i `kjenteBrudd`. Er et kjent brudd
   rettet, fjern det fra listen.
4. Kjør de målrettede testene for hver endret fil, mest spesifikt først. `coreTest` tar ikke `--tests`; bruk
   `./gradlew :flyt:test --tests "…"`. Klassene i venstre kolonne er utagget og trenger ikke Docker. Gir Gradle
   «No tests found for given includes», finnes ikke testen: skriv den, eller si i rapporten at den mangler. Med flere
   `--tests` feiler Gradle bare når ingen av dem treffer, så sjekk at hver klasse du nevner faktisk kjørte.

   | Endret | Uten Docker | Med Docker (steg 5) |
   |---|---|---|
   | `forretningsflyt/steg/**/XxxSteg.kt` | `--tests "*XxxStegTest"` | `--tests "*Flyttest"` |
   | `avklaringsbehov/**` (løser, løsning) | `--tests "*XxxLøserTest" --tests "*AvklaringsbehovsLøserTest" --tests "*AvklaringsbehovLøsningTest"` | `--tests "*AvklaringsbehovOrkestratorTest" --tests "*Flyttest"` |
   | `fordeler/regler/**`, `FordelerRegelService.kt` | `--tests "*XxxRegelTest" --tests "*RegelResultatTest"` | `--tests "*ArenaOppgaveFlytTest"` |
   | `fordeler/arena/**`, `fordeler/Enhetsutreder.kt` | `--tests "*ArenaServiceTest" --tests "*ArenaVideresenderTest" --tests "*EnhetsutrederTest" --tests "*JobbTest" --tests "*JobbUtførerTest"` | `--tests "*ArenaOppgaveFlytTest" --tests "*Flyttest"` |
   | `prosessering/**`, `*JobbUtfører.kt` | `--tests "*XxxJobbUtførerTest"` | `--tests "*Flyttest"` (eneste test av hele Motor-løkka) |
   | `mottak/**` | `--tests "*JoarkKafkaHandlerTest" --tests "*JoarkRegelTest" --tests "*VurderRelevantDokumentForAAPJobbUtførerTest"` | `--tests "*Flyttest"` (eneste test med ekte Kafka) |
   | `faktagrunnlag/**/*Informasjonskrav*.kt`, `*InfoKrav.kt` | `--tests "*JournalpostInformasjonskravTest" --tests "*SaksnummerInfoKravTest"` | `--tests "*Flyttest"` |
   | `klienter/**` | `./gradlew :klienter:coreTest` | – |
   | `repository/**`, Flyway-fil | bare kompilering; si at DB-testene ikke er kjørt | `./gradlew :repository:test`, så `:flyt:test --tests "*RepositoryImplTest"` (tre ligger i `flyt`) |
   | `kontrakt/**` | `./gradlew :kontrakt:test`, så `./gradlew :flyt:coreTest` (`DefinisjonTest` og `AvklaringsbehovsLøserTest` ligger i `flyt`); si eksplisitt at kontrakten endres og publiseres ved merge | – |
   | `api/**` | bare kompilering; `api` har ingen tester | `./gradlew :app:test --tests "*ApiTest"` |
   | `app/**` | `./gradlew :app:coreTest` | `./gradlew :app:test --tests "*ApiTest"` |
   | `.github/**`, `.nais/**`, `Dockerfile` | ingen Gradle; vis diffen | CI på PR-en |

5. Docker-testene: sjekk `docker info >/dev/null 2>&1`. Svarer Docker, kjør høyre kolonne. Gjør den ikke det (vanlig
   i agentens sandkasse), kjør dem ikke og list dem i rapporten som «ikke kjørt (ingen Docker)», så mennesket eller CI
   kan kjøre dem. Feiler Testcontainers med Ryuk, «lease does not exist» eller «Connection refused», er det miljøet,
   ikke koden: si det, ikke «fiks» testen.
6. Rapporter: hvilke kommandoer som kjørte, antall tester per kommando (fra Gradle-utskriften eller
   `*/build/test-results/test/*.xml`), hva som feilet med feilmelding ordrett, og hvilke tester som ikke er kjørt.
   Ikke si «alle tester grønne» uten å ha kjørt dem. Ikke endre forventninger i tester for å få dem grønne uten å
   forklare hvorfor.
