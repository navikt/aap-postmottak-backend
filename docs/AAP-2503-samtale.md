# AAP-2503: Samtalelogg fra spesifiseringen

Samtale med GitHub Copilot CLI 5. november 2026. Den endelige spesifikasjonen ligger i
[AAP-2503-soknad-aap-i-utlandet.md](AAP-2503-soknad-aap-i-utlandet.md).

Loggen gjengir brukerens meldinger og agentens svar. Utdata fra verktøyene (kodesøk og fillesing) og
systeminstruksjonene er utelatt. Svarene er ordrette, bortsett fra at stiene til spesifikasjonen er erstattet med
lenken over.

---

## 1. Bruker

> Vi skal begynne på AAP-2503.
>
> La oss begynne i aap-postmottak-backend. Vi ønsker å journalføre søknader om å beholde AAP i utlandet (dokumenter
> med brevkode NAV 11-03.07) automatisk. I dag blir de journalført manuelt, noe som skaper merarbeid for
> saksbehandlerne.
>
> Et forslag er å journalføre automatisk, og deretter sende dokumentet til behandlingsflyt, og opprette en
> oppfølgingsoppgave slik at saksbehandlerne får en påminnelse om at de må gjøre "noe" manuelt. Men vi er skeptisk til
> at saksbehandlerne skal håndtere dokumentene manuelt, da vi mister kontroll på at de gjør det de skal.
> Hjelp meg med å spesifisere denne oppgaven.

## 1. Agent

Spesifikasjon: [AAP-2503-soknad-aap-i-utlandet.md](AAP-2503-soknad-aap-i-utlandet.md)

Jeg foreslår å droppe oppfølgingsoppgaven. Behandlingsflyt har allerede det som trengs, og ingen kode er endret ennå.

**Forslaget:** Postmottak journalfører dokumentet automatisk og sender det til behandlingsflyt som et annet relevant
dokument (`ANNET_RELEVANT_DOKUMENT`) med vurderingsbehovet `OPPHOLDSKRAV`. Kelvin reiser da avklaringsbehovet
`AVKLAR_OPPHOLDSKRAV` i `VurderOppholdskravSteg`, og saksbehandleren må løse det før behandlingen kan avsluttes. Da har
dere kontroll på at søknaden blir vurdert, og vurderingen havner i statistikken.

**I postmottak-backend** følger vi samme mønster som automatisk journalføring av klage:
- ny brevkode `NAV 11-03.07` og en ny toggle
- tema og sak avklares maskinelt når personen har nøyaktig én Kelvin-sak
- digitalisering og overlevering skjer automatisk

**Dette må avklares før vi implementerer:**
- **Åpen førstegangsbehandling:** `VurderOppholdskravSteg` finnes bare i revurderingsflyten. Hva skjer med
  `OPPHOLDSKRAV` hvis saken har en åpen førstegangsbehandling? Det må avklares med behandlingsflyt.
- **Åpen revurdering eller ingen løpende ytelse:** Hvordan skal søknaden håndteres i disse tilfellene?
- **Ettersending:** Skal `NAVe 11-03.07` også journalføres automatisk?
- **Behandlingstype og ruting:** Skal søknaden få behandlingstypen UTLAND?
- **Strukturert data:** Finnes det en strukturert versjon (JSON) av skjemaet?
- **Frontend:** `OPPHOLDSKRAV` mangler som årsak saksbehandleren kan velge i postmottak-frontenden. Skal den legges til
  for tilfellene som fortsatt behandles manuelt?

Spesifikasjonen har også akseptansekriterier og en liste over testene som trengs.

---

## 2. Bruker

> Dette er en god idé, men det er lokalkontoret (SAKSBEHANDLER_OPPFØLGING) som skal vurdere
> utenlandsoppholdsøknaden. Vi skal vurdere hvorvidt personen får lov til å oppholde seg i utlandet, og hvor mange
> dager. OPPHOLDSKRAV skal vurderes dersom personen ikke har fått godkjent utenlandsopphold, men reiser til utlandet
> allikevel. Dette er derfor ikke relatert til denne oppgaven.

## 2. Agent

Spesifikasjonen er skrevet om etter det du sa. `OPPHOLDSKRAV` er tatt ut, og ingen kode er endret ennå.

**Kelvin har ikke noe for dette i dag.** Det finnes ingen behandling eller avklaringsbehov for å vurdere en søknad om
utenlandsopphold. Det nærmeste er oppfølgingsbehandlingen: den løses av lokalkontoret, men vurderingen lagres bare som
fritekst. Godkjent eller ikke, antall dager, periode og land blir dermed ikke lagret slik at de kan brukes senere, for
eksempel når `OPPHOLDSKRAV` skal vurderes. Det blir heller ikke vedtak eller brev. Skepsisen deres er derfor berettiget.

**Forslaget er å dele oppgaven i to:**
- **Del 1 (postmottak):** journalfør automatisk når personen har nøyaktig én Kelvin-sak, bak en toggle, etter samme
  mønster som klage. Send så søknaden til behandlingsflyt som en ny innsendingstype, `SØKNAD_UTENLANDSOPPHOLD`, i
  stedet for som oppfølgingsoppgave.
- **Del 2 (behandlingsflyt):** behandlingsflyt bestemmer selv hvordan søknaden følges opp. Den kan starte med en
  oppfølgingsbehandling og senere få en egen behandling der lokalkontoret lagrer vurderingen og sender vedtaksbrev,
  etter samme mønster som aktivitetsplikt 11-7. Da trenger ikke postmottak å endres når del 2 kommer.

**Disse spørsmålene må dere ta stilling til:**
- Skal postmottak sende en oppfølgingsoppgave eller den nye innsendingstypen? Den nye typen krever en kontraktsendring
  i behandlingsflyt først.
- Hvordan behandles søknaden i dag, og hvilket vedtak og brev skal bruker få?
- Skal ettersendinger (`NAVe 11-03.07`) også journalføres automatisk?
- Skal søknaden ha behandlingstype UTLAND, og hvilken enhet skal den rutes til?
- Har skjemaet en strukturert versjon (JSON) med periode og land?
- Hva gjør vi når personen ikke har en løpende ytelse, for eksempel ved avslag eller en førstegangsbehandling som ikke
  er ferdig?

---

## 3. Bruker

> Del 1: Siden vi kun kan ha én sak med status UTREDES, vil vi alltid kunne journalføre automatisk. Ikke journalfør på
> saker med status "TRUKKET". Del 2: Jeg er skeptisk til å bruke oppfølgingsoppgave, fordi vi ikke får lagret ned hva
> saksbehandler har gjort. Vi ønsker at brevet sendes manuelt utenfor løsningen, fordi vi ikke har tid til å lage
> brevmal for dette caset, men vi ønsker at saksbehandler bekrefter at de har sendt det. Ettersendinger er utenfor
> scopet til denne oppgaven.

## 3. Agent

Spesifikasjonen er oppdatert etter svarene dine. Oppfølgingsoppgaven og ettersendingene er tatt ut. Ingen kode er
endret.

**Del 1 (postmottak):**
- Søknaden journalføres automatisk på personens Kelvin-sak som ikke er trukket (`resultat != TRUKKET`). Det følger samme
  mønster som klage og ligger bak en toggle.
- Vi bruker ikke `finnEllerOpprettSak`, siden den kan opprette en ny sak.
- Har personen bare trukne saker, journalføres søknaden manuelt som i dag (`AVKLAR_SAK`). Har personen ingen
  Kelvin-sak, går den til Arena som i dag. Fordelingsregelen `KelvinSakRegel` regner også trukne saker som
  Kelvin-saker, så det første tilfellet havner i Kelvin og må fanges i `AvklarSakSteg`.
- Postmottak sender dokumentet til behandlingsflyt som en ny innsendingstype, `SØKNAD_UTENLANDSOPPHOLD`.

**Del 2 (behandlingsflyt):**
- En egen behandlingstype, `Utenlandsopphold`, etter mønsteret fra aktivitetsplikt 11-7.
- Et nytt avklaringsbehov, `VURDER_SØKNAD_UTENLANDSOPPHOLD`, som bare lokalkontoret (`SAKSBEHANDLER_OPPFOLGING`) kan
  løse.
- Vurderingen lagres strukturert: søknadsperiode, land, resultat, godkjente dager og begrunnelse.
- Løsningen avvises hvis ikke `brevSendtManuelt = true`. Det lagres hvem som bekreftet.

**Rekkefølge:** kontraktsendringen i behandlingsflyt må publiseres før postmottak kan bruke den. Spesifikasjonen har
også akseptansekriterier, tester og en oversikt over berørte repoer.

**Disse spørsmålene må avklares:**
1. Hvilken sak velges når personen har flere saker som ikke er trukket? Jeg foreslår saken som dekker mottaksdatoen,
   ellers den nyeste.
2. Skal vurderingen godkjennes av en beslutter (to-trinn)? I så fall bør bekreftelsen på at brevet er sendt komme
   etter vedtaket.
3. Kan én søknad gjelde flere land eller perioder?
4. Hva skjer hvis det kommer en ny søknad mens en utenlandsopphold-behandling er åpen?
5. Hvilken enhet skal oppgaven rutes til, og skal journalposten ha behandlingstype UTLAND?

---

## 4. Bruker

> Hvordan tenkte du at brevSendtManuelt = true blir satt?

## 4. Agent

Saksbehandler setter `brevSendtManuelt` selv, med en påkrevd avkrysning i vurderingsskjemaet i aap-saksbehandling, for
eksempel «Jeg bekrefter at brev om utfallet er sendt til bruker».

**Slik flyter det:**
1. Saksbehandler fyller ut vurderingen og krysser av.
2. Frontenden sender `brevSendtManuelt: true` i løsningen til `POST /løs-behov`.
3. Løseren i behandlingsflyt avviser løsningen med `UgyldigForespørselException` (HTTP 400) hvis feltet ikke er `true`.
   Da kan ikke behandlingen avsluttes.
4. Grunnlaget lagrer bekreftelsen sammen med hvem som løste behovet og når.

**Svakheten:** dette er bare en egenerklæring. Kelvin kan ikke se om brevet faktisk ble sendt. Vi vet hvem som
bekreftet og når, men ikke at det stemmer.

**Hvis dere vil ha mer kontroll:** be saksbehandler oppgi journalpost-ID-en til brevet som ble sendt. Et manuelt sendt
brev blir journalført som utgående journalpost i Joark. Kelvin kan da sjekke i SAF at journalposten finnes, at den er
utgående, at den ligger på saken og at den er journalført etter at søknaden kom inn. Jeg fant ingen SAF-klient i
behandlingsflyt, så dette krever en ny integrasjon.

**Rekkefølgen er ikke avklart:** hvis vurderingen skal godkjennes av en beslutter (to-trinn), bør bekreftelsen være et
eget avklaringsbehov etter vedtaket. Ellers kan brevet bli sendt før vedtaket er godkjent. Uten to-trinn kan
avkrysningen ligge i samme skjema.

Spesifikasjonen er ikke oppdatert med dette. Si fra hvilken variant dere vil ha, så legger jeg den inn.

---

## 5. Bruker

> Er dette samme avklaringsbehov som vurdering av søknaden?

## 5. Agent

Ja, i forrige utkast var brevbekreftelsen et felt i samme løsning som vurderingen. Det bør heller være to
avklaringsbehov i hvert sitt steg. Spesifikasjonen er oppdatert med det.

**Flyten:** `VurderSøknadUtenlandsoppholdSteg` (`VURDER_SØKNAD_UTENLANDSOPPHOLD`) → eventuelt `FatteVedtakSteg` →
`BekreftBrevSendtSteg` (`BEKREFT_BREV_SENDT_MANUELT`) → `AvsluttBehandlingSteg`. Begge behovene løses av
`SAKSBEHANDLER_OPPFOLGING`.

**Hvorfor to steg:**
- Brevet sendes først når vurderingen er endelig. Med to-trinn betyr det etter at beslutter har godkjent.
- Endres vurderingen, må brevbekreftelsen gjøres på nytt.
- Hver bekreftelse har sin egen tidsstempel og saksbehandler, så vi ser hvem som har vurdert og hvem som har sendt
  brevet.
- Oppgavelisten kan skille mellom «må vurderes» og «venter på brev».

Vi trenger et nytt avklaringsbehov for brevbekreftelsen. Dagens brevbehov (`SKRIV_BREV`, `SKRIV_VEDTAKSBREV`) er laget
for brev som produseres i Kelvin, og passer ikke når brevet sendes manuelt.

Løsningen for brevbekreftelsen har feltene `brevSendt` (en påkrevd avkrysning; `false` avvises) og `journalpostIdBrev`
(valgfritt, for kontroll i SAF), i tillegg til hvem som bekreftet og når.

---

## 6. Bruker

> Vi skal ikke ha beslutter. Vi trenger heller ikke journalpostIdBrev da vi implementerer en minimumsløsning (vi skal
> sende brev fra behandlingsflyt på sikt)

## 6. Agent

Spesifikasjonen er oppdatert: beslutter og `journalpostIdBrev` er fjernet, og begge står under «Utenfor scope», sammen
med kontroll av at brevet faktisk er sendt.

**Flyten:** `StartBehandlingSteg` → `VurderSøknadUtenlandsoppholdSteg` → `BekreftBrevSendtSteg` →
`AvsluttBehandlingSteg`.

Jeg har beholdt vurdering og brevbekreftelse som to separate avklaringsbehov, selv uten beslutter. Grunnen er at når
dere senere sender brevet fra behandlingsflyt, kan `BekreftBrevSendtSteg` byttes ut med et vanlig brevsteg (som
`MeldingOmVedtakBrevSteg` i aktivitetsplikt). Vurderingssteget og grunnlaget trenger da ikke å endres.

Løsningen for brevbekreftelsen er nå bare en påkrevd avkrysning (`brevSendt`). Hvem som bekreftet og når, lagres
automatisk.

**Fem spørsmål gjenstår:**
1. Hvilken sak velges når personen har flere saker som ikke er trukket?
2. Kan én søknad gjelde flere land eller perioder?
3. Hva skjer hvis det kommer en ny søknad mens en utenlandsopphold-behandling er åpen?
4. Hvilken enhet skal oppgaven rutes til, og skal journalposten ha behandlingstype UTLAND?
5. Hvordan skal saksbehandler se søknadsdokumentet fra behandlingen?

---

## 7. Bruker

> 1. Det vil ikke skje at en person har flere saker som ikke er trukket. I så fall kan du kaste en feil. 2. Vi starter
> med støtte for ett land og én periode. 3. Vi skal kunne behandle flere utenlandsoppholdsøknader i parallell. Hva er
> problemet med å ha flere åpne behandlinger av denne typen samtidig? 4. Dette er ikke relevant. 5. De kan åpne
> dokumentet manuelt fra dokumentlisten

## 7. Agent

Spesifikasjonen er oppdatert med svarene dine:
1. Har personen flere saker som ikke er trukket, kastes en feil med `error(…)`. Det er lagt til en test for tilfellet.
2. Grunnlaget har ett land og én periode.
3. Hver søknad gir en ny behandling, og flere kan være åpne samtidig (se under).
4. Enhetsruting og behandlingstype i Joark står nå under «Utenfor scope».
5. Saksbehandler åpner dokumentet fra dokumentlisten. Også dette står under «Utenfor scope».

**Parallelle behandlinger er ikke noe problem i seg selv.** Behandlingsflyt gjør det allerede:
`opprettOppfølgingsbehandling` lager alltid en ny behandling (med `forrigeBehandlingId = null`), uavhengig av om det
finnes åpne behandlinger fra før. Tre ting må likevel være på plass:
- **Grunnlaget gjelder én behandling.** Det skal ikke kopieres mellom behandlinger, slik det gjøres for
  ytelsesbehandlinger.
- **Behandlingen må vise hvilken søknad den gjelder** (journalpost-ID og mottatt dato). Saksbehandler åpner dokumentet
  manuelt, og med flere søknader på saken må det være lett å finne riktig dokument.
- **Duplikater:** samme journalpost må ikke gi to behandlinger hvis den sendes på nytt. Sjekk at mottaket avviser den på
  `InnsendingReferanse`. Det har jeg ikke kontrollert.

**Ett spørsmål gjenstår:** klage har toggelen `KunEnAktivKlagebehandling`, som stopper parallelle klagebehandlinger.
Jeg vet ikke hvorfor den ble innført. Spør behandlingsflyt-teamet om den samme grunnen gjelder for utenlandsopphold.

---

## 8. Bruker

> Enhetsruting og behandlingstype skal følge samme logikk som f.eks. vanlig søknad. Det er ingen grunn til at alt dette
> skal kopieres fra klage

## 8. Agent

Spesifikasjonen er rettet. Den har ingen henvisninger til klage lenger, og ingen åpne spørsmål.

**Enhetsruting og behandlingstype:** samme logikk som for vanlig søknad (`NAV 11-13.05`):
- Behandlingstypen i `Brevkoder` er `null`, som for `SØKNAD`.
- `Enhetsutreder` finner enheten i NORG på samme måte som for søknad, ut fra behandlingstema AAP, geografisk
  tilknytning, diskresjonskode og om personen er Nav-ansatt.
- Det lages ingen egen logikk for enhet eller behandlingstype. Punktet er fjernet fra «Utenfor scope».

**Dette er tatt ut:**
- Henvisningen til mønsteret for automatisk klagejournalføring i del 1.
- Det åpne spørsmålet om klagetoggelen `KunEnAktivKlagebehandling`.

Parallelle behandlinger er fortsatt basert på hvordan `BehandlingService` oppretter oppfølgingsbehandlinger i dag. Det
handler bare om hvordan behandlingsflyt lager en ny behandling, ikke om noe fra klage.

---

## 9. Bruker

> Kopier planen til repoet

## 9. Agent

Spesifikasjonen ligger nå i `aap-postmottak-backend/docs/AAP-2503-soknad-aap-i-utlandet.md`. Repoet hadde ingen
dokumentasjonsmappe, så jeg opprettet `docs/`. Filen er ikke committet.

Jeg fikk ikke sjekket git-status, fordi git ikke får lese `~/.gitconfig-nav` i dette miljøet.

---

## 10. Bruker

> kopier hele denne samtalen til en fil i repoet

## 10. Agent

Samtalen er lagret i denne filen.
