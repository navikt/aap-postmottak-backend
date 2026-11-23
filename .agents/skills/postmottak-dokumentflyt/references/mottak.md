# Mottak: fra Kafka-event til Fordeling-behandling

Kode: `flyt/src/main/kotlin/no/nav/aap/postmottak/mottak/`.

## Kafka-topologi (`KafkaStreams.kt`, `JoarkKafkaHandler`)

- Topic `teamdokumenthandtering.aapen-dok-journalfoering` (Avro `JournalfoeringHendelseRecord`), konsumert med Kafka Streams, exactly-once, application id `${KAFKA_STREAMS_APPLICATION_ID}-v2`. Eventdokumentasjon: Confluence "Joarkhendelser".
- Filtre, i rekkefølge: `journalpostStatus == "MOTTATT"`, deretter `mottaksKanal != "EESSI"`. Events for journalposter som allerede er journalført, når aldri postmottak. Postmottak får derfor bare vite om journalføring gjort andre steder ved å lese SAF (`JournalpostInformasjonskrav`) når en behandling prosesseres.
- `split()` grener, første treff vinner. Records som ikke treffer noen av dem, forkastes:
  1. `erTemaEndretFraAAP` (`temaGammelt == AAP && temaNytt != AAP`) → `håndterTemaendring`
  2. `erTemaAAP` (`temaNytt == AAP`) → `nyJournalpost`
- Lokalt (`NAIS_CLUSTER_NAME=LOCAL`) brukes en `NoopStream` i stedet. TestApp har `POST /test/simulerJournalpostHendelse`, som sender en record gjennom den ekte topologien med en `TopologyTestDriver`.

### håndterTemaendring

Én transaksjon: hvis `hentÅpenJournalføringsbehandling(journalpostId)` (type Journalføring, status OPPRETTET eller UTREDES) finnes, legges `ProsesserBehandlingJobbUtfører` i kø. Ingenting annet: prosesseringen oppdager det nye temaet via SAF. Den tar **ikke** behandlingen av Gosys-vent (se `postmottak-flyt-motor/references/avklaringsbehov.md`).

### nyJournalpost

1. Skrivebeskyttet transaksjon: finn en åpen Journalføring-behandling.
2. Funnet → transaksjon: `AvklaringsbehovOrkestrator.taAvVentPgaGosys` (løser `VENT_PA_GOSYS` hvis det er åpent, og sender en hendelse) + legg prosessering i kø.
3. Ikke funnet → `opprettFordelingRegelJobb`:
   - **AAP-retur:** `temaGammelt` er ikke tomt og ikke AAP, den nyeste behandlingen (høyeste id) er en Journalføring med status AVSLUTTET, og tema-vurderingen er ikke AAP/OPP. Da leses SAF. Hvis journalposten fortsatt har tema AAP og status `MOTTATT`, opprettes en ny Journalføring-behandling som prosesseres direkte (ingen Fordeling, ingen ny innkommende journalpost). Ellers ignoreres eventet.
   - Ellers legges `VurderRelevantDokumentForAAPJobbUtfører` i kø med `forSak(journalpostId)` og parameteren `journalpostId`.

Joark sender flere events per journalpost. Gjentatte events er harmløse: jobben er idempotent, og åpne behandlinger prosesseres bare på nytt.

### Oppførsel ved feil

Unntak i handleren fanges ikke. `ProcessingExceptionHandler` svarer `SHUTDOWN_CLIENT`, streamen går i `ERROR`, `/actuator/live` returnerer 503 og poden restartes. Recorden konsumeres på nytt etter restart. Hold denne koden fri for trege eller ustabile kall (bare AAP-retur-grenen kaller SAF).

## VurderRelevantDokumentForAAPJobbUtfører (`vurderdokument.innkommende`)

Kjører én gang per journalpost (gjør ingenting hvis `innkommende_journalpost` allerede har journalposten).

| SAF-journalpost | Registrert status / årsak | Handling |
| --- | --- | --- |
| `JOURNALFOERT` | `IGNORERT` / `ALLEREDE_JOURNALFØRT` | ingen |
| `UTGAAR` | `IGNORERT` / `UTGÅTT` | ingen |
| `bruker` mangler eller mangler id | `GOSYS_FDR` / `MANGLER_IDENT` | Gosys-fordelingsoppgave (hvis det ikke finnes en) |
| `bruker.type == ORGNR` | `GOSYS_FDR` / `ORGNR` | Gosys-fordelingsoppgave med orgnr |
| ellers | `EVALUERT` | PDL-oppslag og person-upsert, enhet via `Enhetsutreder`, deretter opprettes **Fordeling**-behandlingen og prosessering legges i kø (`forBehandling(journalpostId, behandlingId)`) |

Den registrerte raden (`InnkommendeJournalpost`) inneholder brevkode og behandlingstema for hoveddokumentet, `enhet`, `brukerId` (ikke for orgnr) og senere `regelresultat` (skrevet av `AvklarFordelingSteg`). Metrikken `journalpost{brevkode}` økes.

- «Hoveddokument» er her `SafJournalpost.hoveddokument()` = dokumentet med lavest `dokumentInfoId` (strengsammenligning). Andre steder (`Journalpost.hoveddokument`) er det første dokument i lista. De er vanligvis enige, men det er ikke garantert av konstruksjonen.
- `InnkommendeJournalpostStatus.VIDERSENDT_TIL_KELVIN`, `VIDERESENDT_TIL_ARENA` og `GOSYS_JFR` er utfasede verdier som beholdes for gamle rader.

## Enhet (`fordeler/Enhetsutreder.kt`)

`finnJournalføringsenhet(journalpost)`: journalpostens `journalførendeEnhet` hvis den er satt, ikke er `9999` og er aktiv i NORG. Ellers brukes NORG-arbeidsfordeling ut fra PDL-geografisk tilknytning og adressebeskyttelse, NOM egen ansatt, behandlingstema (standard `ab0014`) og behandlingstypen til brevkoden (klage, utland, EØS). Enheten brukes til Arena-oppgaver og Gosys-journalføringsoppgaver. En `null`-enhet sender dokumenter som er rutet til Arena, til manuell journalføring.
