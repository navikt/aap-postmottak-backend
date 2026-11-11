# Data scope, identifiers, integrations and schedules

## Identifiers

| Id | Type | Notes |
| --- | --- | --- |
| journalpostId | `JournalpostId(referanse: Long)` | Joark id. Also used as the motor "sakId" of jobs. |
| behandlingId | `BehandlingId(id: Long)` | Database id. Internal only; never expose it in APIs. |
| behandlingsreferanse | UUID (`Behandlingsreferanse`, `BehandlingsreferansePathParam`) | External id used by the frontend, aap-oppgave and the API paths `/api/behandling/{referanse}`. |
| saksnummer | String | Kelvin sak in aap-behandlingsflyt. |
| innkommendeJournalpostId | Long | Database id of `INNKOMMENDE_JOURNALPOST`, carried in Arena job payloads. |
| person | `Person(id, identifikator: UUID, identer)` | One row per person with all known identer from PDL; `aktivIdent()` is the current one. |

## What is stored where

Schema: `repository/src/main/resources/flyway/` (`V1.<n>__beskrivelse.sql`). Implementations: `repository/src/main/kotlin/no/nav/aap/postmottak/repository/`.

| Data | Scope | Tables | Written by | Copied to DokumentHåndtering |
| --- | --- | --- | --- | --- |
| Behandling, steg history | behandling | `BEHANDLING`, `STEG_HISTORIKK` | engine | no |
| Avklaringsbehov | behandling | `AVKLARINGSBEHOV`, `AVKLARINGSBEHOV_ENDRING`, `AVKLARINGSBEHOV_ENDRING_AARSAK` | engine, API | no |
| Journalpost snapshot | **journalpost**, append-only, newest wins | `JOURNALPOST`, `DOKUMENT`, `AVSENDERMOTTAKER` | `JournalpostInformasjonskrav` | shared, not copied |
| Person and identer | person | `PERSON`, `PERSON_IDENT` | `PersonService` (PDL) | shared |
| Innkommende journalpost, regelresultat | **journalpost** | `INNKOMMENDE_JOURNALPOST`, `REGELSETT_RESULTAT`, `REGEL_EVALUERING` | `VurderRelevantDokumentForAAPJobbUtfører`, `AvklarFordelingSteg` | shared |
| Fordeling vurdering | behandling | `AVKLAR_FORDELING_VURDERING`, `AVKLAR_FORDELING_GRUNNLAG` | `AvklarFordelingSteg`, løser | yes (but Fordeling → Journalføring copies nothing) |
| Tema vurdering | behandling | `TEMAVURDERING`, `TEMAVURDERING_GRUNNLAG` | `AvklarTemaSteg`, løser | yes |
| Saksvurdering | behandling | `SAKSNUMMER_AVKLARING`, `SAKSVURDERING_GRUNNLAG` | `AvklarSakSteg`, løser | yes |
| Kelvin saker snapshot | behandling | `INNHENTEDE_SAKER_FOR_BEHANDLING`, `SAKER_PAA_BEHANDLING` | `SaksnummerInfoKrav` | yes (latest snapshot) |
| Digitaliseringsvurdering | behandling | `DIGITALISERINGSAVKLARING`, `DIGITALISERINGSVURDERING_GRUNNLAG` | `DigitaliserDokumentSteg`, løser | yes |
| Overlevering vurdering | behandling | `OVERLEVERING_VURDERING`, `OVERLEVERING_GRUNNLAG` | `OverleverTilFagsystemSteg`, løser | yes |
| Manual override to Arena | ident | `MANUELL_FORDELING` | by hand (SQL) | n/a |
| Jobs | sak/behandling | `JOBB`, `JOBB_HISTORIKK` (+ motor archive and comment tables) | motor | n/a |

Vurderinger use the grunnlag pattern: immutable vurdering rows plus a `*_GRUNNLAG(BEHANDLING_ID, <vurdering>_ID, AKTIV)` link table. Saving deactivates the old link and adds a new one; reading joins on `AKTIV`; copying adds links for the new behandling to the same vurdering rows.

Pitfalls:
- `JournalpostRepository.hentHvisEksisterer(behandlingId)` joins through `BEHANDLING.JOURNALPOST_ID`, so it returns the newest snapshot regardless of which behandling stored it.
- `BehandlingRepository.hent(journalpostId)` returns an arbitrary behandling when a journalpost has several. Use `hentAlleBehandlingerForJournalpost` or `hentÅpenJournalføringsbehandling`.
- Prod has a logical-replication publication `FOR ALL TABLES` (datastream, `V1.51`/`V1.52`). New tables need a primary key, or UPDATE/DELETE on them fails in prod only.

## External systems

| Gateway (interface in `flyt`) | Implementation (`klienter`) | System | Env var prefix | Used for |
| --- | --- | --- | --- | --- |
| `JournalpostGateway` | `SafGraphqlClientCredentialsClient` | SAF GraphQL | `INTEGRASJON_SAF_URL_GRAPHQL` | reading journalposter (app token) |
| `JournalpostOboGateway` | `SafGraphqlOboClient` | SAF GraphQL | same | reading on behalf of the saksbehandler (API) |
| `DokumentGateway` / `DokumentOboGateway` | `SafRestClient` / `SafOboRestClient` | SAF REST | `INTEGRASJON_SAF_URL_REST` | document files (original JSON, PDFs for the frontend) |
| (class) `JournalføringService` | in `flyt/gateway` | dokarkiv | `INTEGRASJON_JOARK_URL` | oppdater journalpost, ferdigstill, endre tema, kopier |
| `DoksikkerhetsnettGateway` | `DoksikkerhetsnettGatewayImpl` | dokarkiv | `INTEGRASJON_JOARK_URL` | MOTTATT AAP journalposter older than N days (avstemming) |
| `BehandlingsflytGateway` | `BehandlingsflytKlient` | aap-behandlingsflyt (Kelvin) | `INTEGRASJON_BEHANDLINGSFLYT_URL` | finn/opprett sak, finn saker, send hendelse, klagebehandlinger |
| `OppgaveGateway` | `OppgaveKlient` | aap-oppgave (Kelvin oppgaver) | `INTEGRASJON_OPPGAVE_URL` | `DokumentflytStoppetHendelse` |
| `StatistikkGateway` | `StatistikkKlient` | aap-statistikk | `INTEGRASJON_STATISTIKK_URL` | `DokumentflytStoppetHendelse` |
| `GosysOppgaveGateway` | `GosysOppgaveKlient` | Nav Oppgave API (Gosys) | `INTEGRASJON_OPPGAVEAPI_URL` | JFR/FDR and endre-tema oppgaver |
| `PersondataGateway` | `PdlGraphqlKlient` | PDL | `INTEGRASJON_PDL_URL` | identer, fødselsdato, adressebeskyttelse, geografisk tilknytning |
| `NorgGateway` | `NorgKlient` | NORG2 | `INTEGRASJON_NORG_URL` | arbeidsfordeling, active enheter |
| `EgenAnsattGateway` | `NomKlient` | skjermede-personer-pip (NOM) | `INTEGRASJON_NOM_URL` | egen ansatt |
| `VeilarbarenaGateway` | `VeilarbarenaKlient` | veilarbarena | `INTEGRASJON_VEILARBARENA_URL` | oppfølgingsenhet |
| `ArenaoppslagGateway` | `ArenaoppslagGatewayImpl` | aap-arenaoppslag | `INTEGRASJON_AAP_ARENAOPPSLAG_PROXY_URL` | Arena history and vedtak (fordeling rules) |
| `ArenaWebservicesGateway` | `ArenaWebservicesGatewayImpl` | aap-fss-proxy → Arena | `INTEGRASJON_AAP_FSS_PROXY_URL` | active sak, Arena oppgaver, kjøreliste |
| `EnhetsregisteretGateway` | `EREGKlient` | EREG | `INTEGRASJON_EREG_URL` | organisation lookup for avsender orgnr |
| `UnleashGateway` | `UnleashService` | Unleash | `UNLEASH_SERVER_API_URL` | feature toggles |

Every `URL` variable has a matching `SCOPE` variable, except NORG (no auth) and Unleash (`UNLEASH_SERVER_API_TOKEN`). Values per cluster are in `.nais/dev-gcp.yaml` and `.nais/prod-gcp.yaml`; `lib-test/.../FakeServers.kt` sets them to local fake servers in tests. Clients are lazily created singletons that read their config on first use.

## Schedules (cron jobs)

| Job | When | What |
| --- | --- | --- |
| `JoarkAvstemmerJobbUtfører` | daily 03:00 | finds unprocessed AAP journalposter older than 5 days (see `fordeling-og-arena.md`) |
| `GjenopptaBehandlingJobbUtfører` | daily 07:00 | resumes behandlinger whose vent frist has passed |
| motor `oppgave.retryFeilede` | 07:00, 12:00, 15:00, 20:00 | sets all `FEILET` jobs back to `KLAR` |

## Metrics worth knowing

`fordeling_videresend{system, erSoknad}`, `joark_hendelse{hendelseType}`, `journalpost{brevkode}`, `arena_journalfoering{type}`, `postmottak_regelresultat{regel, til_kelvin}`, `postmottak_journalposter_ubehandlet{kildesystem}`, `postmottak_retries_exceeded{jobb_type}`, `postmottak_gosys_kall{operasjon}`, plus the motor's `motor_antall_jobber_feilet` and `motor_antall_jobber_klar` (`flyt/.../PrometheusProvider.kt`).
