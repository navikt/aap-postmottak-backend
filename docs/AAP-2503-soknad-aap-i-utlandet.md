# AAP-2503: Automatisk journalføring og vurdering av søknad om å beholde AAP under opphold i utlandet (NAV 11-03.07)

## Bakgrunn
- Dokumenter med brevkode `NAV 11-03.07` journalføres manuelt i postmottak (brevkoden blir `ANNEN`). Det gir
  merarbeid.
- Lokalkontoret (`SAKSBEHANDLER_OPPFOLGING`) vurderer om personen kan oppholde seg i utlandet, og i hvor mange dager.
- Søknaden har ikke noe med `OPPHOLDSKRAV` å gjøre. `OPPHOLDSKRAV` gjelder utenlandsopphold som ikke er godkjent.
- Kelvin har i dag ingen behandling eller avklaringsbehov for å vurdere en slik søknad.

## Mål
1. Postmottak journalfører søknaden automatisk og sender den til Kelvin.
2. Kelvin oppretter en behandling som lokalkontoret må løse. Vurderingen lagres strukturert.
3. Saksbehandler sender brevet manuelt utenfor Kelvin og bekrefter i Kelvin at det er sendt.

## Utenfor scope
- Ettersendinger (`NAVe 11-03.07`). De behandles som i dag.
- Brevmal og brevproduksjon i Kelvin. Det kommer senere.
- Beslutter og to-trinnskontroll.
- Kontroll av at brevet faktisk er sendt (for eksempel ved å slå opp journalposten i SAF).
- Flere land eller perioder i én søknad.
- Visning av søknadsdokumentet i behandlingen. Saksbehandler åpner det fra dokumentlisten.
- Bruk av det godkjente utenlandsoppholdet i vurderingen av `OPPHOLDSKRAV` eller i underveis. Grunnlaget lages slik at
  det kan brukes senere.
- Oppfølgingsoppgave eller oppfølgingsbehandling. Der lagres ikke det saksbehandler har gjort.

---

## Del 1: aap-postmottak-backend
Enhetsruting og behandlingstype følger samme logikk som for vanlig søknad (`NAV 11-13.05`):
- Behandlingstypen i `Brevkoder` er `null`.
- `Enhetsutreder` finner enheten i NORG ut fra behandlingstema AAP, geografisk tilknytning, diskresjonskode og om
  personen er Nav-ansatt.
- Det lages ingen egen logikk for enhet eller behandlingstype.

### Saksvalg
En person kan ha bare én sak med status UTREDES, så søknaden kan alltid journalføres automatisk når personen har en
gyldig Kelvin-sak.
- `Saksinfo` fra `BehandlingsflytGateway.finnSaker` har `resultat: ResultatKode?`, men ikke saksstatus.
- **Regel:** velg Kelvin-saken der `resultat != ResultatKode.TRUKKET`. Saker som er trukket, brukes aldri.
- Har personen flere saker som ikke er trukket, kastes en feil med `error("…")`. Det skal ikke kunne skje. Jobben
  feiler, og det vises i driften. Logg bare `journalpostId` og `behandlingId`.
- Ikke bruk `finnEllerOpprettSak`. Den kan opprette en ny sak.
- Hvis personen ikke har noen sak som kan brukes:
  - Har personen ingen Kelvin-sak, går journalposten til Arena via fordelingen, som i dag.
  - Har personen bare trukne saker, reiser postmottak `AVKLAR_SAK` (manuell journalføring som i dag).

### Endringer
1. **`Brevkoder`**: legg til `SØKNAD_OM_AAP_I_UTLANDET("NAV 11-03.07", null)` (behandlingstype `null`, som `SØKNAD`) og
   `Journalpost.erSøknadOmAapIUtlandet()`.
   Lagrede enum-navn må ikke endres.
2. **Toggle**: `PostmottakFeature.AutomatiskJournalforingSoknadUtland`, også i begge `when` i `FakeUnleash`.
   Toggelen skal være av i prod til del 2 er ute.
3. **Fordeling**: sjekk at `KelvinSakRegel` og `Regelresultat.skalTilKelvin()` sender dokumentet til Kelvin når
   personen har en Kelvin-sak som ikke er trukket. Lag en regeltest for tilfellet.
4. **`AvklarTemaSteg`**: avklar tema AAP maskinelt når toggelen er på og `erSøknadOmAapIUtlandet()`.
5. **`AvklarSakSteg`**: velg sak etter regelen over og lagre `Saksvurdering(saksnummer, opprettetNy = false)`.
   Finnes det ingen sak som kan brukes, reiser steget `AVKLAR_SAK`.
6. **`JournalføringSteg`**: ingen endring. Steget ferdigstiller maskinelt.
7. **`DigitaliserDokumentSteg`**: lagre `Digitaliseringsvurdering(kategori = InnsendingType.SØKNAD_UTENLANDSOPPHOLD,
   strukturertDokument = SøknadUtenlandsoppholdV0(...), digitalisertManueltGjennomPostmottak = false)`.
   Digitaliseringen er alltid maskinell, også når saken ble avklart manuelt i `AVKLAR_SAK`. Den trenger bare
   `mottattDato` fra journalposten og er uavhengig av hvordan saken ble valgt. `DIGITALISER_DOKUMENT` reises
   bare når toggelen er av.
8. **`OverleverTilFagsystemSteg`**: legg `SØKNAD_UTENLANDSOPPHOLD` til i settet som overleveres automatisk, og håndter
   den nye meldingstypen i `utledMelding`. Den nye kontraktsversjonen gjør `when` ufullstendig, så kompilatoren sier
   fra.
9. **Kontrakt**: oppgrader `aap-behandlingsflyt-kontrakt` til versjonen fra del 2.

### Tester
- Stegtester for `AvklarTemaSteg`, `AvklarSakSteg`, `DigitaliserDokumentSteg` og `OverleverTilFagsystemSteg`, med
  toggelen av og på.
- `AvklarSakSteg` må testes med:
  - én sak
  - én trukket sak og én sak som ikke er trukket
  - flere saker som ikke er trukket (feil kastes)
  - bare trukne saker
  - ingen saker
- Ugyldig journalpost, og journalpost som alt er journalført utenfor Kelvin.
- Et `Flyttest`-scenario fra Kafka-melding til `sendHendelse` med `SØKNAD_UTENLANDSOPPHOLD`. Det krever Docker og
  kjøres i CI.

### Akseptansekriterier
- [ ] `NAV 11-03.07` journalføres automatisk på personens Kelvin-sak som ikke er trukket, uten avklaringsbehov i
      postmottak.
- [ ] Søknaden journalføres aldri på en sak med resultatet `TRUKKET`.
- [ ] Behandlingsflyt mottar `SØKNAD_UTENLANDSOPPHOLD` på riktig sak.
- [ ] Med toggelen av er oppførselen den samme som i dag.
- [ ] `NAVe 11-03.07` behandles som i dag.

---

## Del 2: aap-behandlingsflyt (og berørte repoer)
Egen behandlingstype etter mønsteret fra `Aktivitetsplikt` (11-7). Den løses av lokalkontoret og påvirker ikke
ytelsesbehandlingene.

### Kontrakt (`aap-behandlingsflyt-kontrakt`, bare nye verdier)
- `InnsendingType.SØKNAD_UTENLANDSOPPHOLD`.
- Ny melding `SøknadUtenlandsoppholdV0` (sealed interface + V0), med `mottattDato`. Valider den i `MottattHendelseDto` (`requireNotNull(melding)`).
- `ÅrsakTilOpprettelse.SØKNAD_UTENLANDSOPPHOLD`.
- `TypeBehandling.Utenlandsopphold("utenlandsopphold")`.
- `Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD`, både i intern og i statistikk-enum.
- `Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD` med ny `AvklaringsbehovKode`, `BehovType.MANUELT_PÅKREVD`,
  `løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)` og ny `StegType`.
- `Definisjon.BEKREFT_BREV_SENDT_MANUELT` med ny `AvklaringsbehovKode`, `BehovType.MANUELT_PÅKREVD`,
  `løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)` og ny `StegType`. Dagens brevbehov (`SKRIV_BREV`,
  `SKRIV_VEDTAKSBREV`) er knyttet til brevproduksjon i Kelvin og passer ikke her.

### Flyt `Utenlandsopphold`
`StartBehandlingSteg` → `VurderSøknadUtenlandsoppholdSteg` → `BekreftBrevSendtSteg` → `AvsluttBehandlingSteg`.
Behandlingen har ikke beslutter eller to-trinnskontroll.

Dette er en minimumsløsning. På sikt skal brevet sendes fra behandlingsflyt. Da byttes `BekreftBrevSendtSteg` ut med
et vanlig brevsteg (for eksempel `MeldingOmVedtakBrevSteg`, som i `Aktivitetsplikt`). Vurderingssteget og grunnlaget
endres ikke.

Vurdering og brevbekreftelse er to separate avklaringsbehov i hvert sitt steg. Det har tre fordeler:
- Brevet sendes først når vurderingen er lagret.
- Hvis vurderingen endres, må brevbekreftelsen gjøres på nytt.
- Oppgavelisten skiller mellom «må vurderes» og «venter på brev».
- Steget kan senere byttes ut med automatisk brev uten at vurderingen må endres.

Hver mottatt `SØKNAD_UTENLANDSOPPHOLD` gir en ny behandling. Flere utenlandsopphold-behandlinger kan være åpne på
samme sak samtidig. Det følger samme mønster som `opprettOppfølgingsbehandling` i `BehandlingService`
(`forrigeBehandlingId = null`).
- Legg til en egen gren i `BehandlingService.finnEllerOpprettBehandling`, slik at søknaden ikke havner i
  ytelsesbehandlingen.
- Grunnlaget gjelder én behandling. Det skal ikke kopieres fra eller til andre behandlinger.
- Behandlingen viser hvilken søknad den gjelder (journalpost-ID og mottatt dato). Da kan saksbehandler finne og åpne
  riktig dokument i dokumentlisten når det ligger flere søknader på saken.
- Samme journalpost skal ikke gi to behandlinger hvis den sendes på nytt. Kontroller at
  `HendelseMottattHåndteringJobbUtfører` avviser duplikater på `InnsendingReferanse`.

### Løsning og grunnlag (`SøknadUtenlandsoppholdGrunnlag`, med `aktiv`-flagg, aldri UPDATE)
| Felt | Type | Merknad |
|---|---|---|
| `søknadsperiode` | `Periode` | Perioden personen søker om. Én periode i første versjon. |
| `land` | `String` (ISO 3166) | Ett land i første versjon |
| `resultat` | `INNVILGET` / `DELVIS_INNVILGET` / `AVSLÅTT` | |
| `godkjentPeriode` / `antallGodkjenteDager` | `Periode?` / `Int?` | Påkrevd ved innvilgelse |
| `begrunnelse` | `String` | Påkrevd |
| `vurdertAv` / `vurdertTidspunkt` | | Settes automatisk |

### Løsning for `BEKREFT_BREV_SENDT_MANUELT` (eget grunnlag eller egen tabell)
| Felt | Type | Merknad |
|---|---|---|
| `brevSendt` | `Boolean` | Settes med en påkrevd avkrysning i frontenden. Løseren avviser `false` med `UgyldigForespørselException`. |
| `bekreftetAv` / `bekreftetTidspunkt` | | Settes automatisk |

Behandlingen kan ikke avsluttes før saksbehandler har bekreftet at brevet er sendt manuelt.

### Berørte repoer
| Repo | Endring |
|---|---|
| aap-behandlingsflyt | Kontrakt, flyt, steg, løser, grunnlag + repository + Flyway, API for grunnlaget |
| aap-saksbehandling | Visning av den nye behandlingstypen og skjema for vurderingen med bekreftelse av brev |
| aap-oppgave | Ny behandlingstype og nytt avklaringsbehov til lokalkontor-køen. Ruting til riktig enhet. |
| aap-statistikk | Ny `TypeBehandling`, `ÅrsakTilOpprettelse` og `Vurderingsbehov` (fullstendige `when`) |
| aap-postmottak-backend | Del 1 |

### Akseptansekriterier
- [ ] Mottatt `SØKNAD_UTENLANDSOPPHOLD` oppretter en behandling av typen `Utenlandsopphold` med
      `VURDER_SØKNAD_UTENLANDSOPPHOLD`.
- [ ] Oppgaven havner hos lokalkontoret og kan bare løses av `SAKSBEHANDLER_OPPFOLGING`.
- [ ] Vurderingen lagres strukturert, med resultat, periode eller dager, land og begrunnelse.
- [ ] Etter vurderingen reises `BEKREFT_BREV_SENDT_MANUELT`. Behandlingen kan ikke avsluttes før brevet er bekreftet
      sendt.
- [ ] Ytelsesbehandlinger påvirkes ikke.

---

## Rekkefølge
1. Del 2, kontrakt: nye enum-verdier og ny melding publiseres. Varsle oppgave, statistikk og tilgang.
2. Del 1: postmottak tar i bruk den nye kontrakten. Toggelen er av i prod.
3. Del 2: flyt, frontend, oppgave og statistikk.
4. Slå på toggelen i dev, test ende til ende, og slå den på i prod.

## Åpne spørsmål
Ingen.
