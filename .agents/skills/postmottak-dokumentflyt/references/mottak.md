# Mottak: from Kafka event to Fordeling behandling

Code: `flyt/src/main/kotlin/no/nav/aap/postmottak/mottak/`.

## Kafka topology (`KafkaStreams.kt`, `JoarkKafkaHandler`)

- Topic `teamdokumenthandtering.aapen-dok-journalfoering` (Avro `JournalfoeringHendelseRecord`), consumed with Kafka Streams, exactly-once, application id `${KAFKA_STREAMS_APPLICATION_ID}-v2`. Event documentation: Confluence "Joarkhendelser".
- Filters, in order: `journalpostStatus == "MOTTATT"`, then `mottaksKanal != "EESSI"`. Events for already journalført journalposts never reach postmottak, so postmottak learns about journalføring done elsewhere only by reading SAF (`JournalpostInformasjonskrav`) when a behandling is processed.
- `split()` branches, the first match wins; records matching neither are dropped:
  1. `erTemaEndretFraAAP` (`temaGammelt == AAP && temaNytt != AAP`) → `håndterTemaendring`
  2. `erTemaAAP` (`temaNytt == AAP`) → `nyJournalpost`
- Locally (`NAIS_CLUSTER_NAME=LOCAL`) a `NoopStream` is used instead; TestApp offers `POST /test/simulerJournalpostHendelse` to push a record through the real topology with a `TopologyTestDriver`.

### håndterTemaendring

One transaction: if `hentÅpenJournalføringsbehandling(journalpostId)` (type Journalføring, status OPPRETTET or UTREDES) exists, enqueue `ProsesserBehandlingJobbUtfører`. Nothing else: the processing notices the new tema through SAF. It does **not** take the behandling off Gosys-vent (see `postmottak-flyt-motor/references/avklaringsbehov.md`).

### nyJournalpost

1. Read-only transaction: find an open Journalføring behandling.
2. Found → transaction: `AvklaringsbehovOrkestrator.taAvVentPgaGosys` (solves `VENT_PA_GOSYS` if open and emits a hendelse) + enqueue processing.
3. Not found → `opprettFordelingRegelJobb`:
   - **AAP-retur:** `temaGammelt` is non-blank and not AAP, the latest behandling (highest id) is a Journalføring with status AVSLUTTET, and its tema vurdering is not AAP/OPP. Then SAF is read; if the journalpost is still tema AAP and `MOTTATT`, a new Journalføring behandling is created and processed directly (no Fordeling, no new innkommende journalpost). If not, the event is ignored.
   - Otherwise enqueue `VurderRelevantDokumentForAAPJobbUtfører` with `forSak(journalpostId)` and parameter `journalpostId`.

Joark sends several events per journalpost. Repeated events are harmless: the job is idempotent and open behandlinger are only re-processed.

### Failure behaviour

Exceptions in the handler are not caught. `ProcessingExceptionHandler` answers `SHUTDOWN_CLIENT`, the stream enters `ERROR`, `/actuator/live` returns 503 and the pod is restarted; the record is consumed again after restart. Keep this code free of slow or flaky calls (only the AAP-retur branch calls SAF).

## VurderRelevantDokumentForAAPJobbUtfører (`vurderdokument.innkommende`)

Runs once per journalpost (no-op if `innkommende_journalpost` already has the journalpost).

| SAF journalpost | Recorded status / årsak | Action |
| --- | --- | --- |
| `JOURNALFOERT` | `IGNORERT` / `ALLEREDE_JOURNALFØRT` | none |
| `UTGAAR` | `IGNORERT` / `UTGÅTT` | none |
| `bruker` missing or without id | `GOSYS_FDR` / `MANGLER_IDENT` | Gosys fordelingsoppgave (if none exists) |
| `bruker.type == ORGNR` | `GOSYS_FDR` / `ORGNR` | Gosys fordelingsoppgave with orgnr |
| otherwise | `EVALUERT` | PDL lookup and person upsert, enhet via `Enhetsutreder`, then create the **Fordeling** behandling and enqueue processing (`forBehandling(journalpostId, behandlingId)`) |

The recorded row (`InnkommendeJournalpost`) holds brevkode and behandlingstema of the hoveddokument, `enhet`, `brukerId` (not for orgnr) and later the `regelresultat` (written by `AvklarFordelingSteg`). Metric `journalpost{brevkode}` is incremented.

- "Hoveddokument" here is `SafJournalpost.hoveddokument()` = the dokument with the lowest `dokumentInfoId` (string compare). Elsewhere (`Journalpost.hoveddokument`) it is the first dokument in the list. They usually agree, but not by construction.
- `InnkommendeJournalpostStatus.VIDERSENDT_TIL_KELVIN`, `VIDERESENDT_TIL_ARENA` and `GOSYS_JFR` are deprecated values kept for old rows.

## Enhet (`fordeler/Enhetsutreder.kt`)

`finnJournalføringsenhet(journalpost)`: the journalpost's `journalførendeEnhet` if set, not `9999` and active in NORG; otherwise NORG arbeidsfordeling from PDL geografisk tilknytning and adressebeskyttelse, NOM egen ansatt, behandlingstema (default `ab0014`) and the behandlingstype of the brevkode (klage, utland, EØS). The enhet is used for Arena oppgaver and Gosys journalføringsoppgaver. A `null` enhet sends Arena-routed documents to manual journalføring.
