# Fordeling (Kelvin eller Arena) og Arena-løpet

Kode: `flyt/src/main/kotlin/no/nav/aap/fordeler/` (regler, Arena) og `flyt/.../postmottak/forretningsflyt/steg/fordeling/`.

## AVKLAR_FORDELING (`AvklarFordelingSteg`)

1. En vurdering finnes allerede → `Fullført`.
2. Ingen innkommende journalpost: bare tillatt når SAF-bruker er et orgnr (gamle behandlinger). Lagrer `IGNORERT` og avslutter. Ellers `IllegalArgumentException`.
3. Regelresultat: gjenbruk det lagrede, eller evaluer reglene én gang og lagre resultatet på den innkommende journalposten.
4. For en søknad (hoveddokumentets brevkode SØKNAD): `ArenaService.harOppbruktRettighet` sjekker om søknaden er «kant-til-kant» (personen har brukt opp retten til AAP i Arena). Feil fra Arena kaster unntak, så steget feiler og jobben prøver på nytt. Resultatet logges og telles i metrikken `postmottak_kant_i_kant_oppbrukt_rettighet` (tags `resultat`, `fordelt_til`) uansett hva toggle sier.
5. Lagre `KELVIN` hvis `regelresultat.skalTilKelvin()`, eller hvis toggle `KantTilKantV1` er på og kant-til-kant-resultatet er `skalTilKelvin()`. Ellers `ARENA`. Alt vurdert av SYSTEMBRUKER. Steget oppretter aldri noe avklaringsbehov: manuell fordeling (Definisjon AVKLAR_FORDELING, 1343) er fjernet, og definisjonen er utfaset og beholdes bare for historiske rader.

### Kant-til-kant (`fordeler/arena/ArenaService.harOppbruktRettighet`)

Basert på siste sak og vedtak fra aap-arenaoppslag (`historikkStatusDetalj`). Vurderes i rekkefølge:

| Vilkår | Resultat | Til Kelvin |
| --- | --- | --- |
| Ingen sak, eller saken er verken arbeidsavklaring (UA) eller arbeidsutprøving (AU) | `IkkeRelevantSøknad` | nei |
| Siste vedtak starter etter mottattdato | `IkkeRelevantSøknad` | nei |
| Maksdato eller vedtakets sluttdato oppfyller ikke tidsvilkårene | `IkkeRelevantSøknad` | nei |
| Unntak etter § 11-12 innvilget og vedtakets `til` ≈ `maxdatoUnntak` | `HarBruktOppForlengelsen` | ja |
| Unntak etter § 11-12 innvilget, ikke ved slutten av det | `VedtattForlenget` | nei |
| Vedtakets `til` ikke ≈ `maxdatoOrdinaer` | `IkkeRelevantSøknad` | nei |
| Unntaksvilkårene etter § 11-12 ikke oppfylt | `VedtattAtIkkeForlenges` | ja |
| alt annet | `IkkeRelevantSøknad` | nei |

«≈» betyr at datoene sammenlignes med en toleranse. Gjeldende tidsvilkår og toleranse finnes i `ArenaService`; grensetilfeller testes i `ArenaServiceTest`.

## Regler (`fordeler/regler/`)

| Regel | Aktiv prod / dev+test | `true` betyr | Datakilde |
| --- | --- | --- | --- |
| `KelvinSakRegel` | ja / ja | personen har minst én Kelvin-sak | behandlingsflyt `finnSaker` |
| `ArenaSakRegel` | ja / ja | personen har AAP-historikk i Arena. Bare logget, brukes ikke i avgjørelsen. | arenaoppslag |
| `ArenaHistorikkRegel` | ja / ja | **ingen** vesentlig AAP-historikk i Arena | arenaoppslag |
| `ErIkkeReisestønadRegel` | ja / ja | hoveddokumentet er ikke en søknad om reisestønad eller en ettersendelse | brevkode |
| `ErIkkeAnkeRegel` | ja / ja | hoveddokumentet er ikke en anke | brevkode |
| `ManueltOverstyrtTilArenaRegel` | ja / ja | en av personens identer er overstyrt til Arena i tabellen `MANUELL_FORDELING` | database |
| `Aldersregel` | ja / **nei** | personen er minst 18 (usann hvis fødselsdato er ukjent) | PDL |
| `SøknadRegel` | ja / **nei** | hoveddokumentet er en søknad | brevkode |

Nye regler: implementer `Regel<T>` med et `companion object : RegelFactory<T>` (`erAktiv`, `medDataInnhenting`). `FordelerRegelService` finner dem gjennom det sealed hierarkiet. Hvis du legger til eller deaktiverer en regel som `Regelresultat.skalTilKelvin()` nevner eksplisitt, må du oppdatere den funksjonen også (kommentarene i reglene sier det).

### `Regelresultat.skalTilKelvin()`

1. `ManueltOverstyrtTilArenaRegel` true → **Arena**.
2. `KelvinSakRegel && ErIkkeReisestønadRegel && ErIkkeAnkeRegel` → **Kelvin** (en eksisterende Kelvin-sak vinner over Arena-historikk, alder og dokumenttype).
3. Ellers **Kelvin** bare hvis alle regler unntatt `KelvinSakRegel`, `ArenaSakRegel` og `ManueltOverstyrtTilArenaRegel` er true. Ellers **Arena**. I prod betyr dette: voksen, søknad, ingen vesentlig Arena-historikk, ikke reisestønad, ikke anke.

Merknader:
- `FordelerRegelService.evaluer` kaller `vurder()` to ganger per regel (én gang for en metrikk, én gang for resultatet), så hver regels oppslag skjer to ganger.
- Det lagrede regelresultatet (`REGELSETT_RESULTAT.SYSTEM_NAVN`) er utfallet av reglene. Avgjørelsen er `AVKLAR_FORDELING_VURDERING`. Historiske manuelle fordelinger kan avvike fra regelresultatet.

## AVKLAR_FORDELING_VIDERESEND (`FordelingVideresendSteg`)

| Vurdering | Handling |
| --- | --- |
| `KELVIN` | Opprett en `Journalføring`-behandling og legg prosessering i kø. Ingenting kopieres fra Fordeling-behandlingen. |
| `ARENA` | `ArenaVideresender.videresendJournalpostTilArena` |
| `IGNORERT` eller ingen | bare logg |
| `BEGGE` | kaster unntak: støttes ikke ennå |

## Arena-løpet (`fordeler/arena/ArenaVideresender.kt`, `fordeler/arena/jobber/`)

`ArenaVideresender` leser journalposten på nytt fra SAF og gjør ingenting hvis den er `JOURNALFOERT` eller `UTGAAR`. Uten enhet på den innkommende journalposten går den rett til manuell journalføring. Ellers, etter hoveddokumentets brevkode:

| Brevkode | Handling |
| --- | --- |
| LEGEERKLÆRING | Journalføres synkront på generell sak med tema **OPP**, deretter ferdigstilles den (ingen Arena-oppgave) |
| SØKNAD | jobb `SendSøknadTilArenaJobbUtfører` |
| STANDARD_ETTERSENDING, SØKNAD_OM_REISESTØNAD | jobb `OppprettOppgaveIArenaJobbUtfører` |
| SØKNAD_OM_REISESTØNAD_ETTERSENDELSE | jobb `SendTilArenaKjørelisteBehandling` (kommentar i koden: virker ikke i dag, mangler XML) |
| alt annet | jobb `ManuellJournalføringJobbUtfører` |

Alle Arena-jobber har nøkkel `forSak(journalpostId)` med en JSON-payload `ArenaVideresenderKontekst`, arver fra `ArenaJobbutførerBase` (hopper stille over med mindre journalposten fortsatt er `MOTTATT`) og faller tilbake til `ManuellJournalføringJobbUtfører` i stedet for å feile:

```text
SendSøknadTilArena:      ingen enhet, eller Arena har allerede en aktiv sak for personen → Manuell
                         ellers opprett STARTVEDTAK-oppgave i Arena → AutomatiskJournalføring(arenaSakId)
OppprettOppgaveIArena:   nyeste aktive Arena-sak → opprett BEHENVPERSON-oppgave → AutomatiskJournalføring
SendTilArenaKjøreliste:  behandleKjoerelisteOgOpprettOppgave → AutomatiskJournalføring
AutomatiskJournalføring: journalfør på Arena-fagsaken (fagsystem AO01) → ferdigstill → lukk Gosys AAP-oppgaver
ManuellJournalføring:    enhet kjent og < 3 tidligere feil → Gosys JFR-oppgave for enheten; ellers Gosys FDR-oppgave
```

Grenser for retry og reserveløsninger står i `postmottak-flyt-motor/references/motor-jobs-transactions.md`. Kall til Arena-webtjenester går gjennom aap-fss-proxy (`ArenaWebservicesGateway`). Lesing fra Arena går gjennom aap-arenaoppslag (`ArenaoppslagGateway`).

## Sikkerhetsnett: `JoarkAvstemmerJobbUtfører` (cron 03:00)

`JoarkAvstemmer.avstem()` henter AAP-journalposter som fortsatt er `MOTTATT` etter 5 dager fra dokarkiv (unntatt EESSI) og sjekker hver av dem mot det lagrede regelresultatet:

| Situasjon | Handling |
| --- | --- |
| Ingen regelresultat og en Gosys JFR-oppgave finnes | ingenting |
| Ingen regelresultat, opprettet før 2025-04-01 | ingenting (før Kelvin) |
| Ingen regelresultat | Gosys FDR-oppgave, telles som uavstemt |
| Regelresultat KELVIN | telles som uavstemt, ingen handling (feillogg i prod) |
| Regelresultat ARENA, ingen Gosys-oppgave | Gosys JFR-oppgave (retry uten enhet og behandlingstema, reserve FDR) |

`GET /api/dokumenter/finn-ubehandlede` lister de uavstemte Kelvin-journalpostene med referansen til den åpne Journalføring-behandlingen deres.
