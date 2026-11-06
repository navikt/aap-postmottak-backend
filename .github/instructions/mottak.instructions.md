---
applyTo: "flyt/src/main/kotlin/no/nav/aap/postmottak/mottak/**"
description: "Kafka-mottaket fra Joark: filtre, transaksjoner, feil stopper konsumenten, VurderRelevantDokument og innkommende_journalpost"
---
# Regler for mottaket (Kafka fra Joark)

- Topic `teamdokumenthandtering.aapen-dok-journalfoering`, Avro `JournalfoeringHendelseRecord`. Topologien i
  `KafkaStreams.kt`: filter på `journalpostStatus == MOTTATT` og kanal ikke `EESSI`, deretter to grener
  (tema endret fra AAP → `håndterTemaendring`, tema AAP → `nyJournalpost`). Det finnes ingen brevkodefilter her.
- Mottaket gjør minst mulig: slå opp åpen behandling, ta av vent, legg en jobb i kø. Arbeidet skjer i jobbene
  (`VurderRelevantDokumentForAAPJobbUtfører`, `ProsesserBehandlingJobbUtfører`), som har retries.
- Databasearbeid går via `TransactionProvider.inTransaction { … }`; åpne ikke egne transaksjoner.
- Et uventet unntak under prosessering gir `SHUTDOWN_CLIENT` (`StreamsException.kt`): konsumenten stopper og
  journalposter blir liggende. Håndter forventede feil eksplisitt, og kast aldri for data du kan hoppe over.
  Deserialiseringsfeil stopper også strømmen.
- Detaljer om feil logges til `team-logs` (secure log); vanlig logg får bare type og «se secure-logs».
- `VurderRelevantDokumentForAAPJobbUtfører` setter status på `innkommende_journalpost`
  (`InnkommendeJournalpostStatus`, lagret i DB: bare legg til, deprecate gamle verdier som i enumen i dag).
- Lokalt (`Miljø` LOKALT) brukes `NoopStream`; `TestApp` simulerer hendelser via `/test/simulerJournalpostHendelse`.
- Test: `JoarkKafkaHandlerTest` med `TopologyTestDriver` (ingen broker, kjører i `coreTest`) for filtre og grener;
  `Flyttest` med ekte Kafka-container for hele veien (Docker, menneske/CI).
