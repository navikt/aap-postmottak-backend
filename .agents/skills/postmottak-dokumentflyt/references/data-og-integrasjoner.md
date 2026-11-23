# Dataomfang, identifikatorer, integrasjoner og tidsplaner

## Identifikatorer

| Id | Type | Merknader |
| --- | --- | --- |
| journalpostId | `JournalpostId(referanse: Long)` | Joark-id. Brukes også som motorens «sakId» for jobber. |
| behandlingId | `BehandlingId(id: Long)` | Database-id. Bare intern, må aldri eksponeres i API-er. |
| behandlingsreferanse | UUID (`Behandlingsreferanse`, `BehandlingsreferansePathParam`) | Ekstern id som frontend, aap-oppgave og API-stiene `/api/behandling/{referanse}` bruker. |
| saksnummer | String | Kelvin-sak i aap-behandlingsflyt. |
| innkommendeJournalpostId | Long | Database-id for `INNKOMMENDE_JOURNALPOST`, med i payloaden til Arena-jobber. |
| person | `Person(id, identifikator: UUID, identer)` | Én rad per person med alle kjente identer fra PDL. `aktivIdent()` er den gjeldende. |

## Hva som lagres hvor

Skjema: `repository/src/main/resources/flyway/` (`V1.<n>__beskrivelse.sql`). Implementasjoner: `repository/src/main/kotlin/no/nav/aap/postmottak/repository/`.

| Data | Omfang | Tabeller | Skrevet av | Kopiert til DokumentHåndtering |
| --- | --- | --- | --- | --- |
| Behandling, steghistorikk | behandling | `BEHANDLING`, `STEG_HISTORIKK` | motoren | nei |
| Avklaringsbehov | behandling | `AVKLARINGSBEHOV`, `AVKLARINGSBEHOV_ENDRING`, `AVKLARINGSBEHOV_ENDRING_AARSAK` | motoren, API | nei |
| Journalpost-snapshot | **journalpost**, kun tillegg, nyeste vinner | `JOURNALPOST`, `DOKUMENT`, `AVSENDERMOTTAKER` | `JournalpostInformasjonskrav` | delt, ikke kopiert |
| Person og identer | person | `PERSON`, `PERSON_IDENT` | `PersonService` (PDL) | delt |
| Innkommende journalpost, regelresultat | **journalpost** | `INNKOMMENDE_JOURNALPOST`, `REGELSETT_RESULTAT`, `REGEL_EVALUERING` | `VurderRelevantDokumentForAAPJobbUtfører`, `AvklarFordelingSteg` | delt |
| Fordelingsvurdering | behandling | `AVKLAR_FORDELING_VURDERING`, `AVKLAR_FORDELING_GRUNNLAG` | `AvklarFordelingSteg`, løser | ja (men Fordeling → Journalføring kopierer ingenting) |
| Tema-vurdering | behandling | `TEMAVURDERING`, `TEMAVURDERING_GRUNNLAG` | `AvklarTemaSteg`, løser | ja |
| Saksvurdering | behandling | `SAKSNUMMER_AVKLARING`, `SAKSVURDERING_GRUNNLAG` | `AvklarSakSteg`, løser | ja |
| Snapshot av Kelvin-saker | behandling | `INNHENTEDE_SAKER_FOR_BEHANDLING`, `SAKER_PAA_BEHANDLING` | `SaksnummerInfoKrav` | ja (siste snapshot) |
| Digitaliseringsvurdering | behandling | `DIGITALISERINGSAVKLARING`, `DIGITALISERINGSVURDERING_GRUNNLAG` | `DigitaliserDokumentSteg`, løser | ja |
| Overleveringsvurdering | behandling | `OVERLEVERING_VURDERING`, `OVERLEVERING_GRUNNLAG` | `OverleverTilFagsystemSteg`, løser | ja |
| Manuell overstyring til Arena | ident | `MANUELL_FORDELING` | for hånd (SQL) | ikke relevant |
| Jobber | sak/behandling | `JOBB`, `JOBB_HISTORIKK` (+ motorens arkiv- og kommentartabeller) | motoren | ikke relevant |

Vurderinger følger grunnlag-mønsteret: uforanderlige vurderingsrader pluss en koblingstabell `*_GRUNNLAG(BEHANDLING_ID, <vurdering>_ID, AKTIV)`. Lagring deaktiverer den gamle koblingen og legger til en ny. Lesing joiner på `AKTIV`. Kopiering legger til koblinger for den nye behandlingen til de samme vurderingsradene.

Fallgruver:
- `JournalpostRepository.hentHvisEksisterer(behandlingId)` joiner gjennom `BEHANDLING.JOURNALPOST_ID`, så den returnerer det nyeste snapshotet uavhengig av hvilken behandling som lagret det.
- `BehandlingRepository.hent(journalpostId)` returnerer en vilkårlig behandling når en journalpost har flere. Bruk `hentAlleBehandlingerForJournalpost` eller `hentÅpenJournalføringsbehandling`.
- Prod har en publication for logisk replikering `FOR ALL TABLES` (datastream, `V1.51`/`V1.52`). Nye tabeller trenger primærnøkkel, ellers feiler UPDATE/DELETE på dem bare i prod.

## Eksterne systemer

| Gateway (interface i `flyt`) | Implementasjon (`klienter`) | System | Prefiks for miljøvariabel | Brukes til |
| --- | --- | --- | --- | --- |
| `JournalpostGateway` | `SafGraphqlClientCredentialsClient` | SAF GraphQL | `INTEGRASJON_SAF_URL_GRAPHQL` | lese journalposter (app-token) |
| `JournalpostOboGateway` | `SafGraphqlOboClient` | SAF GraphQL | samme | lese på vegne av saksbehandleren (API) |
| `DokumentGateway` / `DokumentOboGateway` | `SafRestClient` / `SafOboRestClient` | SAF REST | `INTEGRASJON_SAF_URL_REST` | dokumentfiler (original-JSON, PDF-er til frontend) |
| (klasse) `JournalføringService` | i `flyt/gateway` | dokarkiv | `INTEGRASJON_JOARK_URL` | oppdater journalpost, ferdigstill, endre tema, kopier |
| `DoksikkerhetsnettGateway` | `DoksikkerhetsnettGatewayImpl` | dokarkiv | `INTEGRASJON_JOARK_URL` | MOTTATT AAP-journalposter eldre enn N dager (avstemming) |
| `BehandlingsflytGateway` | `BehandlingsflytKlient` | aap-behandlingsflyt (Kelvin) | `INTEGRASJON_BEHANDLINGSFLYT_URL` | finn/opprett sak, finn saker, send hendelse, klagebehandlinger |
| `OppgaveGateway` | `OppgaveKlient` | aap-oppgave (Kelvin-oppgaver) | `INTEGRASJON_OPPGAVE_URL` | `DokumentflytStoppetHendelse` |
| `StatistikkGateway` | `StatistikkKlient` | aap-statistikk | `INTEGRASJON_STATISTIKK_URL` | `DokumentflytStoppetHendelse` |
| `GosysOppgaveGateway` | `GosysOppgaveKlient` | Nav Oppgave-API (Gosys) | `INTEGRASJON_OPPGAVEAPI_URL` | JFR/FDR og endre-tema-oppgaver |
| `PersondataGateway` | `PdlGraphqlKlient` | PDL | `INTEGRASJON_PDL_URL` | identer, fødselsdato, adressebeskyttelse, geografisk tilknytning |
| `NorgGateway` | `NorgKlient` | NORG2 | `INTEGRASJON_NORG_URL` | arbeidsfordeling, aktive enheter |
| `EgenAnsattGateway` | `NomKlient` | skjermede-personer-pip (NOM) | `INTEGRASJON_NOM_URL` | egen ansatt |
| `VeilarbarenaGateway` | `VeilarbarenaKlient` | veilarbarena | `INTEGRASJON_VEILARBARENA_URL` | oppfølgingsenhet |
| `ArenaoppslagGateway` | `ArenaoppslagGatewayImpl` | aap-arenaoppslag | `INTEGRASJON_AAP_ARENAOPPSLAG_PROXY_URL` | Arena-historikk og vedtak (fordelingsregler) |
| `ArenaWebservicesGateway` | `ArenaWebservicesGatewayImpl` | aap-fss-proxy → Arena | `INTEGRASJON_AAP_FSS_PROXY_URL` | aktiv sak, Arena-oppgaver, kjøreliste |
| `EnhetsregisteretGateway` | `EREGKlient` | EREG | `INTEGRASJON_EREG_URL` | organisasjonsoppslag for avsender-orgnr |
| `UnleashGateway` | `UnleashService` | Unleash | `UNLEASH_SERVER_API_URL` | feature toggles |

Hver `URL`-variabel har en tilsvarende `SCOPE`-variabel, unntatt NORG (ingen autentisering) og Unleash (`UNLEASH_SERVER_API_TOKEN`). Verdier per cluster står i `.nais/dev-gcp.yaml` og `.nais/prod-gcp.yaml`. `lib-test/.../FakeServers.kt` setter dem til lokale fake-servere i tester. Klientene er singletons som opprettes ved første bruk og leser konfigurasjonen da.

## Tidsplaner (cron-jobber)

| Jobb | Når | Hva |
| --- | --- | --- |
| `JoarkAvstemmerJobbUtfører` | daglig 03:00 | finner uprosesserte AAP-journalposter eldre enn 5 dager (se `fordeling-og-arena.md`) |
| `GjenopptaBehandlingJobbUtfører` | daglig 07:00 | gjenopptar behandlinger der ventefristen er passert |
| motor `oppgave.retryFeilede` | 07:00, 12:00, 15:00, 20:00 | setter alle `FEILET`-jobber tilbake til `KLAR` |

## Metrikker det er greit å kjenne til

`fordeling_videresend{system, erSoknad}`, `joark_hendelse{hendelseType}`, `journalpost{brevkode}`, `arena_journalfoering{type}`, `postmottak_regelresultat{regel, til_kelvin}`, `postmottak_journalposter_ubehandlet{kildesystem}`, `postmottak_retries_exceeded{jobb_type}`, `postmottak_gosys_kall{operasjon}`, pluss motorens `motor_antall_jobber_feilet` og `motor_antall_jobber_klar` (`flyt/.../PrometheusProvider.kt`).
