# AAP-2503: Review-rapport

Rapporten samler tilbakemeldingene fra gjennomgangen av koden for søknad om AAP i utlandet, og forklarer hvordan
hver feil kunne vært oppdaget tidligere, helst i spesifikasjonen. Den oppdateres underveis i gjennomgangen.

- Spesifikasjon: [AAP-2503-soknad-aap-i-utlandet.md](AAP-2503-soknad-aap-i-utlandet.md)
- Samtalelogg: [AAP-2503-samtale.md](AAP-2503-samtale.md)
- Siste oppdatering: 6. november 2026

## Fremdrift i gjennomgangen

| Del | Status |
|---|---|
| Behandlingsflyt, commit 1: kontrakten | Gjennomgått, alle punkter er besvart (se «Åpne spørsmål») |
| Behandlingsflyt, commit 2: opprett egen behandling | Gjennomgått, godkjent |
| Behandlingsflyt, commit 3: statistikk | Gjennomgått, godkjent |
| Behandlingsflyt, commit 4: flyten | Del A (flyt, steg, løsere), del B (lagring, repositories) og del C (tester) gjennomgått. API-test lagt til (F14), `NO` avvist (F15). Spørsmålene fra del A om avslag, periode eller dager, tidsstempler og endring av vurdering er ikke besvart |
| Postmottak, commit 1: spesifikasjon og samtalelogg | Hoppet over etter ønske fra utvikleren |
| Postmottak, commit 2: tema og sak | Gjennomgått. Rettet for F16 (papir journalføres også maskinelt). Spørsmålene om hvilke saker som kan brukes (`AVSLAG`, `KLAGE_*`), `error()` mot `AVKLAR_SAK`, tema `!= AAP` og toggelnavn er ikke besvart |
| Postmottak, commit 3 og 4 | Presentert. Spørsmålene (overlevering uten toggle, `digitalisertManueltGjennomPostmottak`, ubrukt `BrevkoderHelper`-mapping, `søknadsdato = null`, `saksnummer!!`, lokal kontraktversjon) er ikke besvart |
| Frontend (aap-saksbehandling) | Presentert. Spørsmålene (datovalidering, tekst i 4402, landlisten og Norge-feilen, redigering av 4401 mens 4402 er åpen, håndskrevne skjemaer) er ikke besvart |
| Statistikk, oppgave og tilgang | Ikke gjennomgått |
| Branches | Arbeidet er flyttet fra `AAP-BAU-ki-oppsett-copilot` til nye branches fra `main` uten AAP-BAU-commitene: `AAP-2503-utenlandsopphold` i behandlingsflyt (Flyway omdøpt til `V1.364`) og i postmottak. Testene er kjørt på nytt og er grønne |

## Oversikt over funn

| Nr. | Funn | Fra | Alvorlighet | Status |
|---|---|---|---|---|
| F1 | Kontrakten har et `fritekst`-felt ingen fyller ut | Utvikler | Middels | Rettet |
| F2 | KDoc-linje for kode 5002 forsvant under konfliktløsning | Agenten | Lav | Rettet |
| F3 | Flyway-migrasjonen fikk et versjonsnummer som allerede var brukt på `main` | Agenten | Middels | Rettet |
| F4 | Spesifikasjonen spurte ikke om hvert nye avklaringsbehov skal totrinnskontrolleres eller kvalitetssikres | Utvikler | Middels | Valget stemmer, prosessen er ikke endret ennå |
| F5 | De to stegene lå i hver sin steggruppe, men skal vises på samme side | Utvikler | Lav | Rettet |
| F6 | Digitaliseringen ble gjort avhengig av at saken var avklart maskinelt, men skal alltid være automatisk | Utvikler | Høy | Rettet |
| F7 | Commit 2 i behandlingsflyt kompilerer ikke alene, fordi den bruker klasser fra commit 4 | Agenten | Lav | Akseptert, ikke rettet |
| F8 | Jobbprioriteten for søknaden ble `NORMAL`, men skal være `LAV` | Utvikler | Lav | Rettet |
| F9 | Brevbekreftelsen (4402) kunne bli «utdatert» av en ny vurdering (4401), men behandlingen avsluttes når 4402 er besvart | Utvikler | Middels | Rettet |
| F10 | Grunnlagsresponsen returnerte `vurdertAv` som streng i stedet for `vurderingerMeta` | Utvikler | Lav | Rettet |
| F11 | Vurderingen i 4401 manglet mellomlagring | Utvikler | Lav | Rettet |
| F12 | `vurdertAv` var `String` i domenet i stedet for `Bruker` | Utvikler | Lav | Rettet |
| F13 | Egen tabell og repository for brevbekreftelsen, som allerede ligger på avklaringsbehovet | Utvikler | Middels | Rettet |
| F14 | Grunnlags-API-et hadde ingen API-test | Utvikler | Lav | Rettet |
| F15 | Backend godtok `NO` som land, selv om søknaden gjelder opphold utenfor Norge | Utvikler | Lav | Rettet |
| F16 | Hjelpefunksjonen het `erAutomatisk…`, og agenten spurte ikke om papir og avveiningen mellom tidsbesparelse og risiko | Utvikler | Middels | Rettet |

Alvorlighet er vurdert ut fra hva det ville kostet å rette feilen etter utgivelse. Et felt i kontrakten er
vanskelig å fjerne, fordi kontrakten bare skal få nye verdier og aldri miste noen.

## F1: `fritekst` i `SøknadUtenlandsoppholdV0`

**Tilbakemelding:** «Hvorfor la du til dette? Det er ikke ønskelig at et menneske skal digitalisere søknaden, fordi
behandlingen av søknaden uansett er manuell, og det ville lagt til et ekstra unødvendig steg (merarbeid).»

**Hva som skjedde:** Meldingen fikk feltet `fritekst` som skulle fylles ut av den som digitaliserte søknaden.
Feltet ble sendt videre til frontenden og vist i vurderingsbildet. Postmottak digitaliserer maskinelt og setter
bare `mottattDato`, så feltet var alltid tomt. Feltet var altså kode uten produsent, og det antydet en manuell
digitalisering som ikke skal finnes.

**Årsak:**
- Spesifikasjonen sa «for eksempel `mottattDato` og eventuell fritekst». Det var et forslag formulert som en
  mulighet, og det ble tatt med uten at noen spurte hvem som skulle fylle ut feltet.
- Spesifikasjonen hadde heller ikke et uttrykkelig prinsipp om at ingen mennesker skal være med på digitaliseringen.
  Uten det virket et felt for manuell tekst naturlig.
- Ingen kontrollpunkt krevde at hvert nytt felt i kontrakten har en som fyller det ut og en som bruker det.

**Hvordan det kunne vært forhindret:**
1. **Tabell over felt i spesifikasjonsmalen.** Hver kontraktendring beskrives med felt, hvem som fyller det ut,
   hvem som leser det og hvorfor. Felt uten både produsent og forbruker tas ikke med.
2. **Spørsmål i `/spesifiser`.** Hver gang spesifikasjonen rører kontrakten, skal agenten spørre «hvem fyller ut
   dette feltet, og hvem bruker det?» før spesifikasjonen godkjennes.
3. **Regel i `kontrakt.instructions.md`.** Legg til: «Nye felt krever en navngitt produsent og forbruker. Kontrakten
   er bare-legg-til, så et ubrukt felt blir værende.» Instruksjonsfilen sier allerede at kontrakten bare skal få
   nye verdier, men sier ikke noe om hva som er nok grunn til å legge til et felt.
4. **Prinsipp i spesifikasjonen.** Skriv «ingen manuelle steg i postmottak» under Utenfor omfang, slik at
   avgrensningen er eksplisitt.
5. **Sjekk i `flyt-review`.** Be den uavhengige gjennomgangen lete etter nye felt i kontrakten som ingen
   produksjonskode setter, og som derfor alltid er `null`.

**Rettet:** Feltet er fjernet fra kontrakten, DTO-en, mappingen, frontenden (visning, testdata og `schema.d.ts`) og
spesifikasjonen. Den lokalt publiserte kontrakten `0.0.673-utland-lokal` har fortsatt feltet, og må publiseres på
nytt før versjonen byttes ut.

## F2: Tapt KDoc-linje i `AvklaringsbehovKode.kt`

**Funnet av:** Agenten, under oppsummeringen før gjennomgangen. Ikke en tilbakemelding fra utvikleren.

**Hva som skjedde:** Under en cherry-pick til en eldre branch oppsto det en konflikt. Konflikten ble løst med et
skript som fjernet alle linjer som nevnte `OVERSTYR_IKKE_OPPFYLT_MELDEPLIKT_KODE`. Det fjernet også KDoc-linjen over
kode `5002`, så den ble stående som en tom kommentar. Feilen ble oppdaget da agenten leste diffen før gjennomgangen.

**Årsak:** Konflikten ble løst med et mønsterbasert skript i stedet for en håndlest endring, og resultatet ble ikke
kontrollert linje for linje.

**Hvordan det kunne vært forhindret:**
1. Løs konflikter med redigeringsverktøyet og les hele diffen etterpå. Se spesielt etter fjernede linjer (`-`) som
   ikke hører til oppgaven.
2. Legg til i `verifiser`-skillen: etter en konfliktløsning, kjør `git diff` mot commiten før og forklar hver
   fjernet linje.
3. Les diffen før den vises til utvikleren. Det er det som fanget feilen, så det bør være et fast steg.

**Rettet:** Linjen er gjenopprettet og slått sammen med kontrakt-commiten.

## F3: Flyway-versjon som kolliderte med `main`

**Funnet av:** Agenten, da testene på den nye branchen feilet.

**Hva som skjedde:** Migrasjonen fikk nummer `V1.357` fordi den eldre branchen ikke hadde fått med seg de nyeste endringene fra `main`. På `main`
fantes allerede `V1.357__etablering_vurdering_versjon.sql`. Alle databasetestene feilet med «Found more than one
migration with version 1.357». Migrasjonen er omdøpt til `V1.362`.

**Årsak:** Versjonsnummeret ble valgt ut fra en branch som lå etter `main`. Samme feil kan skje igjen når to branches
lager migrasjoner samtidig.

**Hvordan det kunne vært forhindret:**
1. Hent `main` og velg versjonsnummeret fra den før migrasjonen skrives.
2. Ikke oppgi versjonsnummer i spesifikasjonen. Skriv «ny migrasjon» under Risiko, og la nummeret velges ved
   bygging.
3. Legg til en sjekk i `verifiser`: finn duplikate Flyway-versjoner med `ls ... | sed 's/__.*//' | sort | uniq -d`.
4. Vurder en test som feiler uten Docker hvis to migrasjoner har samme versjon. Dagens feil kom først i
   databasetestene, som krever Docker.

**Rettet:** Omdøpt til `V1.362`. Det er ingen duplikater igjen. Rekkefølgen må sjekkes på nytt når branchen
rebases på `main`.

## F4: Totrinn og kvalitetssikring ble ikke spurt om

**Tilbakemelding (svar på Q1):** De nye avklaringsbehovene skal ikke totrinnskontrolleres. Det kan man ikke finne ut
av programmatisk, så det bør spørres om hvert avklaringsbehov skal totrinnskontrolleres og/eller kvalitetssikres.
Kvalitetssikring er bare relevant for avklaringsbehov som kan løses av `SAKSBEHANDLER_OPPFOLGING`.

**Hva som skjedde:** `VURDER_SØKNAD_UTENLANDSOPPHOLD` (4401) og `BEKREFT_BREV_SENDT_MANUELT` (4402) har
`kreverToTrinn` og `kvalitetssikres` uendret på standardverdien `false`. Agenten satte aldri verdiene eksplisitt og
la ikke frem valget som et spørsmål. Resultatet ble riktig denne gangen, men bare fordi standardverdien tilfeldigvis
passet.

**Årsak:** Spesifikasjonen nevnte ikke totrinn eller kvalitetssikring. Hvert avklaringsbehov må ha en bevisst
beslutning om dette, siden valget styrer hvem som må se på vurderingen etterpå. Standardverdiene er `false`, så et
glemt valg ser ut som et valg om ikke å kontrollere.

**Hvordan det kunne vært forhindret:**
1. **Sjekkliste i `/spesifiser`.** For hvert nytt avklaringsbehov skal agenten stille to spørsmål: skal det
   totrinnskontrolleres, og skal det kvalitetssikres? Det siste spørres bare om når `løsesAv` inneholder
   `SAKSBEHANDLER_OPPFOLGING`.
2. **Tabell i spesifikasjonsmalen.** Én rad per avklaringsbehov med kode, hvem som løser det, totrinn (ja/nei) og
   kvalitetssikring (ja/nei/ikke relevant). Tabellen skal være utfylt ved godkjenning.
3. **Eksplisitte verdier i `Definisjon`.** Sett `kreverToTrinn` og `kvalitetssikres` uttrykkelig for nye
   definisjoner, også når verdien er `false`, så valget er synlig i koden og i diffen.
4. **Oppskriften `nytt-avklaringsbehov`.** Legg til de to spørsmålene som et eget punkt, slik at de ikke hoppes over.

**Status:** Ingen kodeendring nødvendig. Q1 og Q5 er besvart: ingen totrinnskontroll og ingen kvalitetssikring.

## F5: To steggrupper i stedet for én

**Tilbakemelding (svar på Q2):** Steggruppe er bare relevant for visning. Det er naturlig å spørre hvilke steg som
skal vises på samme side. Her skal stegene vises på samme side.

**Hva som skjedde:** `VURDER_SØKNAD_UTENLANDSOPPHOLD` og `BEKREFT_BREV_SENDT` hadde hver sin steggruppe, og frontenden
måtte slå dem sammen med en egen betingelse for å få én side. Agenten tok steggruppen for et teknisk skille mellom
stegene, ikke et valg om hva saksbehandleren ser.

**Årsak:** Spesifikasjonen sa ikke hvilke steg som hører sammen i visningen. Siden hvert steg krever en
steggruppe, ble det laget én per steg uten å spørre.

**Hvordan det kunne vært forhindret:**
1. **Spørsmål i `/spesifiser`.** For hvert nytt steg: «hvilke steg skal vises på samme side?» Svaret avgjør
   steggruppen.
2. **Tabell i spesifikasjonsmalen.** En rad per steg med steggruppe og hvilken side det vises på. To steg på samme
   side deler gruppe.
3. **Oppskriften `nytt-steg`.** Legg til punktet «tilhører steget en eksisterende gruppe?» før det opprettes en
   ny `StegGruppe`.

**Rettet:** `StegGruppe.BEKREFT_BREV_SENDT` er fjernet, og `StegType.BEKREFT_BREV_SENDT` bruker nå
`StegGruppe.SØKNAD_UTENLANDSOPPHOLD`. Frontenden viser grunnlaget under den ene gruppen, og betingelsen i
`OppgaveKolonne.tsx` er forenklet. `BEKREFT_BREV_SENDT` er også fjernet fra gruppe-enumene i `schema.d.ts`.
Steget er fortsatt et eget `StegType`, så det er uendret for flyten.

## F6: Manuell digitalisering som fallback

**Tilbakemelding (svar på Q4):** Selv om saken ikke kan avklares maskinelt, skal hele digitaliseringen fortsatt skje
automatisk. «Hvorfor tenkte du at man må behandle digitaliseringen manuelt dersom man ikke klarer å avklare sak
maskinelt, når disse er i forskjellige flyter/behandlingstyper?»

**Hva som skjedde:** `DigitaliserDokumentSteg` digitaliserte bare automatisk når `AvklarSakSteg` hadde valgt en
eksisterende sak uten å reise `AVKLAR_SAK`. I alle andre tilfeller reiste steget `DIGITALISER_DOKUMENT`, og en
saksbehandler måtte digitalisere en søknad som ikke har noe å digitalisere.

**Årsak:** Journalføring og digitalisering er egne steg i hver sin behandlingstype. Agenten behandlet dem som én
kjede der en manuell avklaring i det første steget gjør at det neste også må være manuelt. Digitaliseringen av denne
søknaden trenger bare `mottattDato` fra journalposten, så den er uavhengig av hvordan saken ble valgt.
Spesifikasjonen formulerte koblingen som en sikkerhetsregel («bare når saken ble avklart maskinelt»), og den ble
bygget slik uten at noen spurte hva den beskyttet mot.

**Hvordan det kunne vært forhindret:**
1. **Beskrive avhengigheter mellom steg i spesifikasjonen.** Når et steg gjøres betinget av et annet, skal
   spesifikasjonen si hvilken informasjon det trenger fra det andre steget. Her trengte det ingenting.
2. **Tabell over manuelle punkter.** Spesifikasjonsmalen kan kreve en liste over alle steder en saksbehandler kan
   gripe inn, med begrunnelse. «Ingen manuelle steg i postmottak» (se F1) kunne vært sjekket mot den.
3. **Spørsmål i `/spesifiser`.** «Finnes det noen situasjon der et menneske skal inn i dette steget? Hvorfor?» Svaret
   «ja, fordi forrige steg var manuelt» skal utløse oppfølgingsspørsmålet «trenger steget noe fra det forrige?».
4. **Test for hele flyten.** Flyttesten dekket bare den helautomatiske veien. Et scenario med manuell sakavklaring
   ville vist at digitaliseringen havnet i en manuell oppgave.

**Rettet:** Betingelsen og hjelpefunksjonen `erSakAvklartMaskinelt` er fjernet fra `DigitaliserDokumentSteg`.
Søknaden digitaliseres nå automatisk når toggelen er på og dokumentet har brevkoden NAV 11-03.07. Testene som
forventet `DIGITALISER_DOKUMENT` ved manuell eller ny sak er byttet ut med én test som sjekker at digitaliseringen er
automatisk uavhengig av saksvurdering. Spesifikasjonen er oppdatert. Toggelen er uendret: er den av, reises
`DIGITALISER_DOKUMENT` som i dag.

**Oppfølging:** `OverleverTilFagsystemSteg` overleverer automatisk så lenge digitaliseringen finnes, og bruker
saksnummeret fra saksvurderingen. Det er ikke endret.

## F7: Commit 2 kompilerer ikke alene

**Funnet av:** Agenten, ved starten av gjennomgangen av commit 2.

**Hva som skjedde:** Commit 2 («Opprett egen behandling per søknad») refererer til `Utenlandsopphold.flyt()` i
`BehandlingType.kt` og til `SøknadUtenlandsoppholdDokumentDto` i `MottaDokumentService`. Begge ligger i commit 4
(«Flyt for behandling»). Et bygg av commit 2 alene feiler. Det gjør at `git bisect` ikke fungerer over disse commitene,
og at hver commit ikke kan vurderes eller rulles tilbake for seg.

**Årsak:** Alt ble skrevet i ett arbeidstre og delt i commits i ettertid etter tema, uten å bygge hver commit.

**Hvordan det kunne vært forhindret:**
1. Bygg hver commit etter oppdelingen, for eksempel `git rebase -i --exec "./gradlew compileKotlin compileTestKotlin"`.
2. Legg til i `/ferdigstill`-skillen at hver commit skal kompilere alene, og at oppdelingen styres av avhengigheter,
   ikke av tema.

**Status:** Akseptert av utvikler: commitene trenger ikke å kompilere hver for seg. Ingen endring. Tiltakene over
gjelder bare hvis teamet senere ønsker bisect-trygge commits.

## F8: Jobbprioritet `NORMAL` i stedet for `LAV`

**Tilbakemelding:** Prioritet ble satt til `NORMAL`. Prioritet bør være lav. Dette kan være lurt å spørre om.

**Hva som skjedde:** `HendelseMottattHåndteringJobbUtfører.utledPrioritet` har et uttømmende `when` over alle
`InnsendingType`, så kompilatoren krevde et valg for den nye typen. Agenten la den i samme gruppe som `SØKNAD`
(`NORMAL`), fordi det var en søknad. Behandlingen av en søknad om AAP i utlandet haster ikke på samme måte som en
ordinær søknad om AAP, som blokkerer saksbehandlingen av en person som venter på ytelse.

**Årsak:** Valget ble tatt av navn («søknad»), ikke ut fra hvor fort søknaden må håndteres. Spesifikasjonen sa
ingenting om prioritet, og ingen spurte.

**Hvordan det kunne vært forhindret:**
1. **Sjekkliste for ny `InnsendingType` i `/spesifiser`.** Hver ny innsendingstype har valg som kompilatoren
   tvinger frem, men som er forretningsbeslutninger: jobbprioritet, årsak til opprettelse, vurderingsbehov og
   mapping i tilbakekrevingen. Spesifikasjonen skal ta stilling til alle.
2. **Ny oppskrifts-skill `ny-innsendingstype`.** Den lister alle uttømmende `when`-uttrykk over `InnsendingType`
   og spør om valget for hvert av dem, i stedet for at agenten velger det som kompilerer.
3. **Prinsipp:** Når kompilatoren tvinger frem et valg i en `when`, er det et spørsmål til utvikleren, ikke en
   gjetning. Det samme gjelder tilbakekrevingsmappingen til `NYE_OPPLYSNINGER` og `IKKE_RELEVANT` i
   `FlytKontekstMedPeriodeService`, som fortsatt venter på svar.

**Rettet:** `SØKNAD_UTENLANDSOPPHOLD` er flyttet til gruppen med `Prioritet.LAV`. Modulen kompilerer. Endringen ligger
i commit 2.

## F9: Utdatert-logikk for brevbekreftelsen som aldri kan inntreffe

**Tilbakemelding:** I det man har besvart 4402 avsluttes behandlingen. Det er derfor ikke mulig å gå tilbake og
endre 4401 etter at 4402 er godkjent.

**Hva som skjedde:** Agenten antok at vurderingen kunne endres etter at brevet var bekreftet, og bygde logikk for det.
`BekreftBrevSendtSteg` sammenlignet `bekreftetTidspunkt` med `vurdertTidspunkt` og åpnet bekreftelsen på nytt hvis
vurderingen var nyere. Frontenden hadde en tilsvarende sjekk. Spesifikasjonen hadde punktet «Hvis vurderingen endres,
må brevbekreftelsen gjøres på nytt», og det fantes en egen test for det. Når 4402 er besvart, har flyten ingen flere
steg, og behandlingen avsluttes. Da er den avsluttet, og ingen avklaringsbehov kan løses.

**Årsak:** Agenten reiste ikke spørsmålet «kan behandlingen åpnes igjen etter siste steg?». Den leste ikke hvordan
`FlytOrkestrator` avslutter en flyt (`neste == null` gir `lukkBehandling`). Reglene ble også skrevet som en
generell robusthet, uten at noen sjekket at situasjonen kan oppstå. Det ga kode, test og dokumentasjon for et
tilfelle som ikke finnes. Det førte også til en tidsstempel-sammenligning (`LocalDateTime.now()`), som var det som
utløste spørsmålet om rekkefølge og versjon i forrige runde.

**Hvordan det kunne vært forhindret:**
1. **Tilstandsoversikt i spesifikasjonen.** Spesifikasjonsmalen skal ha en tabell over behandlingens status og
   avklaringsbehov per steg, med en rad som sier når behandlingen avsluttes og hva som kan endres etter det.
2. **Spørsmål i `/spesifiser`:** «Hva skjer hvis en tidligere vurdering endres senere? Er det mulig i det hele tatt?»
3. **Sjekkliste i `nytt-steg`:** Les hvordan flyten avsluttes, og ikke legg til logikk for tilfeller flyten ikke kan
   gi. Hvert «hvis X endres» i spesifikasjonen skal ha en forklaring på hvordan X kan skje.
4. **Prinsipp:** Ingen kode eller test for et scenario som ikke kan nås. Scenarioet må kunne beskrives som en
   sekvens av handlinger i grensesnittet.

**Rettet:**
- `BekreftBrevSendtSteg` er nå tilstrekkelig vurdert når en bekreftelse finnes, og avhenger ikke lenger av
  `SøknadUtenlandsoppholdRepository`.
- Testen `endret vurdering etter bekreftelse gjenåpner bekreftelsen` er fjernet.
- Frontenden (`BekreftBrevSendt.tsx`) bruker bekreftelsen direkte.
- Spesifikasjonen sier nå at behandlingen avsluttes når brevet er bekreftet.
- Endringen ligger i commit 4.

**Relatert spørsmål om status (Q6):** Flyten går fra `UTREDES` rett til `AVSLUTTET` uten `IVERKSETTES`. Det er
teknisk riktig og har presedens i `OppfølgingsBehandling`. Svaret over bekrefter at det er ønsket: behandlingen
avsluttes når 4402 er besvart, og det er ikke noe vedtak å iverksette.

## F10: `vurdertAv` som streng i stedet for `vurderingerMeta`

**Tilbakemelding:** Typisk bruker vi `vurderingerMeta` på alle grunnlagsresponser. Hvorfor avvek vi fra dette og
returnerte `vurdertAv`?

**Hva som skjedde:** `SøknadUtenlandsoppholdVurderingResponse` og `BekreftBrevSendtResponse` returnerte
`vurdertAv`/`vurdertTidspunkt` og `bekreftetAv`/`bekreftetTidspunkt` som rå strenger. Frontenden bygde selv et
`VurdertAvAnsatt` av dem, med `kilde` hardkodet til `SAKSBEHANDLER`. Resultatet var at kortet viste Nav-ident i stedet
for navn og enhet, og at kvalitetssikrer og beslutter ikke ville blitt vist hvis de ble innført senere.

**Årsak:** Agenten brukte `AvklarOppfolgingsoppgaveGrunnlagApi` som mal, fordi `OppfølgingsBehandling` er den
nærmeste flyten uten vedtak. Det grunnlaget bruker en `vurdertAv: String`, og frontenden der sender
`vurderingerMeta={undefined}`. Det er altså ikke mønsteret resten av kodebasen følger. Agenten sammenlignet ikke med
flere grunnlags-API-er (for eksempel `RettighetsperiodeGrunnlagApi`) og sjekket ikke om det fantes en felles
mekanisme (`VurdertAvService.byggVurderingerMeta`).

**Hvordan det kunne vært forhindret:**
1. **Mønster i skillen `nytt-grunnlag-api` (eller `nytt-steg`):** Alle grunnlagsresponser med vurderinger skal ha
   `vurderingerMeta` bygd med `VurdertAvService.byggVurderingerMeta`. Skillen skal peke på én kanonisk
   referanse (for eksempel `RettighetsperiodeGrunnlagDto`), ikke på «nærmeste lignende flyt».
2. **Regel for agenten:** Ved kopiering av et mønster skal minst tre eksempler sammenlignes. Avvik mellom dem
   er et spørsmål til utvikleren.
3. **Frontend:** Ikke bygg `VurdertAvAnsatt` for hånd. Bruk `vurderingerMeta` fra responsen.

**Rettet:**
- Begge responsene har nå `vurderingerMeta` (`VurderingerMetaResponse`), bygd med `VurdertAvService`
  (`VURDER_SØKNAD_UTENLANDSOPPHOLD` og `BEKREFT_BREV_SENDT_MANUELT`).
- `søknadUtenlandsoppholdGrunnlagApi` tar `gatewayProvider`, og `App.kt` er oppdatert.
- Frontenden og `schema.d.ts` bruker `vurderingerMeta.vurdertAv`.
- Endringen ligger i commit 4.

## F11: Mellomlagring manglet på 4401

**Tilbakemelding:** 4401 skal ha mellomlagring. Det er ikke vits med mellomlagret vurdering av 4402, fordi det bare
er et enkelt «har du sendt brev»-spørsmål.

**Hva som skjedde:** Frontenden brukte `VilkårskortMedForm` uten mellomlagring for begge stegene. Saksbehandleren
mister da et halvferdig skjema med begrunnelse, land og perioder hvis hen forlater siden. Spesifikasjonen tok ikke
stilling til mellomlagring.

**Årsak:** Mellomlagring er standard for vurderingsskjemaer i frontenden, og det er bare unntaksvis at en vurdering
ikke skal mellomlagres. Agenten behandlet det som et valg og tok den enkleste komponenten som kompilerte
(`VilkårskortMedForm`), i stedet for å bruke mellomlagring som utgangspunkt. Spesifikasjonen sa ingenting om det.

**Hvordan det kunne vært forhindret:**
1. **Standard, ikke spørsmål:** Hvert vurderingsskjema implementeres med mellomlagring
   (`VilkårskortMedFormOgMellomlagring`, `useMellomlagring`, `hentMellomlagring` i data-henteren og
   `nullstillMellomlagretVurdering` ved vellykket løsning). Backend er generisk (nøkkel er avklaringsbehovkoden),
   så det trengs ingen endring der.
2. **Spør bare ved tvil:** Agenten spør hvis skjemaet er et enkelt ja/nei-spørsmål som er raskt å fylle ut på nytt,
   som brevbekreftelsen (4402). Utvikleren avgjør da.
3. **Skriv det i skillene:** Standarden og unntaket skal stå i `nytt-avklaringsbehov` og `nytt-steg`. Da trenger ikke
   spesifikasjonen ta det opp for hvert behov.

**Rettet:**
- 4401 bruker `VilkårskortMedFormOgMellomlagring`. `UtenlandsoppholdMedDataFetching` henter mellomlagringen, og
  `Utenlandsopphold` sender `erIkkePåVent`.
- 4402 er uendret, med `VilkårskortMedForm`.
- Ny test: utkastet vises, og feltene gjenopprettes fra mellomlagringen.
- Spesifikasjonen er oppdatert.

## F12: `vurdertAv` som `String` i domenet

**Tilbakemelding:** Bruk domeneobjekter hvis mulig. `Bruker` er bedre enn `String` for `vurdertAv`.

**Hva som skjedde:** `SøknadUtenlandsoppholdVurdering.vurdertAv` var en `String`, og repoet brukte `setString` og
`getString`. API-et måtte pakke den inn i `Bruker(...)` for å bygge `vurderingerMeta`. Tilsvarende vurderinger i
kodebasen (for eksempel `RettighetsperiodeVurdering`) bruker `Bruker` med `setBruker` og `getBruker`.

**Årsak:** Samme som F10: agenten brukte `String` fordi det var det enkleste og sammenlignet ikke med flere
tilsvarende domeneklasser.

**Hvordan det kunne vært forhindret:** Regelen «bruk domeneobjekter (`Bruker`, `Periode`, `Saksnummer`,
`BehandlingId`) i stedet for primitive typer» skal stå i agentinstruksjonene, med regelen om å sammenligne minst
tre eksempler fra F10. En enkel sjekk er å søke etter `: String` på felt som heter `*Av`, `ident` og `fnr` i nye
domeneklasser.

**Rettet:** `vurdertAv` er `Bruker` i domenet, i løseren, repoet (`setBruker`, `getBruker`) og testene. API-et
trenger ikke lenger å pakke den inn.

## F13: Egen lagring av brevbekreftelsen

**Tilbakemelding:** Det er riktig at vi ikke trenger å lagre ned vurderingen av bekreftelse av brevet, siden dette
allerede lagres ned på avklaringsbehovet.

**Hva som skjedde:** Agenten laget grunnlag, to tabeller, et repository (Postgres og InMemory), registrering og
repository-test for bekreftelsen. Bekreftelsen er alltid `true`, siden løseren avviser `false`. Hvem som bekreftet og
når, finnes dessuten allerede som historikk på avklaringsbehovet.

**Årsak:** Agenten antok at et løst avklaringsbehov må ha et grunnlag, slik de fleste vurderinger har. Den sjekket
ikke om informasjonen allerede lå et annet sted. Spesifikasjonen hadde heller ikke et spørsmål om det («eget
grunnlag eller egen tabell» sto som en åpen overskrift i spesifikasjonen, men ble ikke avgjort).

**Hvordan det kunne vært forhindret:**
1. **Spørsmål i `/spesifiser` og `nytt-avklaringsbehov`:** «Hva er det som lagres, og finnes det allerede på
   avklaringsbehovet (løst av, tidspunkt)?» Rene bekreftelser trenger ikke eget grunnlag.
2. **Precedent:** Steg som `SamordningBarnepensjonSteg` bruker `erTilstrekkeligVurdert = { true }`, og
   `BekreftTotalvurderingKlageLøser` har ingen repoer. Skillen skal peke på dem.
3. **Prinsipp:** Lagre ikke data som ikke har en annen konsument enn et grunnlag ingen leser. Vi hadde alt `løstAv()`
   og `sistAvsluttet()` på avklaringsbehovet, og de brukes nå til `vurderingerMeta`.

**Rettet:**
- Grunnlag, repository (Postgres og InMemory), registreringer, tester og tabellene
  `BEKREFT_BREV_SENDT_BEKREFTELSE` og `BEKREFT_BREV_SENDT_GRUNNLAG` er fjernet. Migrasjonen `V1.362` har bare tabellene
  for vurderingen.
- `BekreftBrevSendtSteg` bruker `erTilstrekkeligVurdert = { true }`. `BekreftBrevSendtLøser` har ingen repoer og
  validerer bare `brevSendt`.
- API-et bygger `bekreftelse` fra avklaringsbehovet (`løstAv()` og `sistAvsluttet()`) når det er løst.
  `BekreftBrevSendtResponse` har bare `vurderingerMeta`.
- Frontenden og `schema.d.ts` er oppdatert. Spesifikasjonen er oppdatert.
- Endringen ligger i commit 4.

## F14: Manglende API-test

**Tilbakemelding:** Selv om vi har dårlig testdekning på API-nivå i dag, ønsker vi å teste API-ene også.

**Hva som skjedde:** Agenten testet steg, løsere, domene og repo, og hele flyten med `FlytOrkestrator`, men ikke
`søknadUtenlandsoppholdGrunnlagApi`. Mange eksisterende grunnlags-API-er har heller ingen test, og agenten tok det som
et tegn på at det ikke var forventet.

**Årsak:** Agenten kopierte testdekningen til de eksisterende eksemplene i stedet for å følge ønsket praksis. Det
sto ingenting om API-tester i spesifikasjonsmalen eller i skillene.

**Hvordan det kunne vært forhindret:** Teststrategien i spesifikasjonen skal ha en egen linje per nytt endepunkt, og
`nytt-grunnlag-api` skal inneholde et eksempel på en API-test med `BaseApiTest`, `@Fakes` og InMemory-repoer.
Dårlig dekning i eksisterende kode er ikke et argument for å utelate testen.

**Rettet:** `SøknadUtenlandsoppholdGrunnlagApiTest` i `api/src/test` har fire tester: søknaden uten vurdering og
bekreftelse, vurderingen med `vurderingerMeta`, ingen bekreftelse mens 4402 er åpen, og bekreftelse med
`vurderingerMeta` fra avklaringsbehovet når brevet er bekreftet. Den fullt kvalifiserte `BehandlingRepository` i
`SøknadUtenlandsoppholdFlytTest` er byttet med en import. Endringen ligger i commit 4.

## F15: Norge godtatt som land

**Tilbakemelding:** `NO` bør ikke aksepteres, ettersom søknaden gjelder opphold i utlandet i forhold til Norge.

**Hva som skjedde:** Valideringen godtok alle ISO 3166-landkoder. Frontenden utelater Norge fra listen
(`alleLandUtenNorgeAlpha2`), men backend sjekket ikke det. Agenten la merke til avviket og stilte det som spørsmål
(Q-b) i stedet for å rette det.

**Årsak:** Regelen sto bare i frontenden. Backend skal ikke stole på at klienten filtrerer, og regler om hva som er
gyldig input skal ligge i domenet. Spesifikasjonen sa bare «ISO 3166».

**Hvordan det kunne vært forhindret:** Spesifikasjonsmalen skal ha en kolonne for gyldige og ugyldige verdier per
inputfelt. Agenten skal sjekke at hver begrensning i frontenden også finnes i backend-valideringen.

**Rettet:** `SøknadUtenlandsoppholdVurdering.valider` avviser `NO` med en tydelig feilmelding. Testen
`Norge avvises som land` er lagt til, og spesifikasjonen er oppdatert. Endringen ligger i commit 4.

## F16: Navnet «automatisk» og antakelsen om digital eller papir

**Tilbakemelding:** `erAutomatiskSøknadOmAapIUtlandet` er et navn som ikke gir mening. Det er kopiert fra
`erAutomatiskKlage`, som heller ikke gir mening, siden søknaden ikke er automatisk. Utvikleren ba først om at bare
digitale søknader skulle journalføres automatisk, «samme mønster som alle andre dokumenter enn klage». Senere angret
utvikleren på det og ga denne konteksten:

- Når vi trenger strukturert data (digitalisering), må papir håndteres manuelt.
- Når vi ikke trenger strukturert data (klage, søknad om AAP i utlandet), kan også papir håndteres automatisk. Et
  menneske saksbehandler dokumentet uansett, og et ekstra digitaliseringssteg som kanskje gjøres av en annen person
  enn saksbehandleren, er bortkastet tid.
- Ulempen er at dokumenter som burde vært stoppet i journalføringen, kan gå videre: uleselig skanning, eller en annen
  persons dokument i samme journalpost som burde vært splittet.
- Det er en avveining mellom tidsbesparelse ved journalføring og merarbeid når noe med feil allerede er journalført.
  Agenten skal derfor få bekreftelse fra utvikleren når den mener et dokument bør journalføres automatisk selv om det
  er papir.

**Hva som skjedde:**
1. Agenten kopierte navnet fra `erAutomatiskKlage` uten å vurdere om det beskrev noe. Det er toggelen og
   dokumenttypen som avgjør, ikke at dokumentet er automatisk.
2. Agenten spurte ikke om papir. Første versjon behandlet papir og digitalt likt (bare brevkode), men agenten hadde
   verken grunngitt avveiningen eller bedt om bekreftelse. Spørsmålet kom først under gjennomgangen, og som et
   enten/eller (digital eller ikke) uten å nevne hva avveiningen er.
3. Etter ønsket om «bare digitale» innførte agenten `erDigital()` på alle tre stegene uten å se at det ville gi manuell
   digitalisering av papir. Det motsier at søknaden ikke trenger strukturert data, og det førte til at digitaliseringen
   fikk et manuelt steg som F6 hadde fjernet. Agenten sa fra om konflikten, men innførte den likevel.

**Årsak:** Agenten sammenlignet med klage og med de digitale dokumenttypene uten å forstå hvorfor de er forskjellige.
Regelen er ikke «digital eller papir» men «trenger vi strukturert data?». Agenten kjente ikke avveiningen mellom
tidsbesparelse og risiko ved papir, og spesifikasjonen sa ikke noe om den.

**Hvordan det kunne vært forhindret:**
1. **Skill for nye dokumenttyper i postmottak** med spørsmålene: «Trenger vi strukturert data fra dokumentet? Kan det
   komme på papir? Hva er avveiningen mellom tidsbesparelse og risiko for feil som burde vært stoppet i
   journalføringen (uleselig skanning, flere personers dokumenter i samme journalpost)? Be utvikleren om å bekrefte
   valget for papir.»
2. **Ta med avveiningen i spesifikasjonen,** under en egen overskrift («Papir og digitalt»). Dette er gjort.
3. **Navnregel:** Navn skal beskrive hva som sjekkes (`skal…Maskinelt`), ikke kopieres fra et eksisterende navn som
   ikke gir mening.
4. **Spør før kode, når en endring motsier en tidligere beslutning** (F6): Skriv motsigelsen som spørsmål, ikke som
   merknad etter at koden er endret.

**Rettet:**
- `Journalpost.erSøknadOmAapIUtlandet()` sjekker bare brevkoden. Både papir og digitale søknader går maskinelt
  gjennom tema, sak og digitalisering.
- Hjelpefunksjonen i `AvklarTemaSteg` heter `skalSøknadOmAapIUtlandetAvklaresMaskinelt` (toggle og brevkode).
- Det er tester for papir i `AvklarTemaSteg`, `AvklarSakSteg`, `DigitaliserDokumentSteg` og `JournalpostTest`, og en
  helautomatisk flyttest både for digital og for papirsøknad. Saf-fakeen gir en digital originalvariant når kanalen
  ikke er papir.
- `erAutomatiskKlage` er ikke endret, siden det ikke hører til denne oppgaven.
- Endringen ligger i postmottak-commit 2 og 4. Spesifikasjonen har en ny overskrift «Papir og digitalt».

## Besvarte spørsmål fra gjennomgangen

| Nr. | Spørsmål | Svar |
|---|---|---|
| Q1 | Totrinnskontroll på 4401 | Nei. Se F4. |
| Q2 | Én eller to steggrupper | Én, stegene vises på samme side. Se F5. |
| Q3 | Navnet `BEKREFT_BREV_SENDT_MANUELT` | OK, beholdes. |
| Q4 | Manuell digitalisering som fallback | Nei. Digitaliseringen er alltid automatisk. Se F6. |
| Q5 | Kvalitetssikring av 4401 og 4402 | Nei. Se F4. |
| Q6 | Kan flyten gå fra `UTREDES` til `AVSLUTTET` uten `IVERKSETTES`? | Ja. Behandlingen avsluttes når 4402 er besvart, og vurderingen kan ikke endres etterpå. Se F9. |
| Q7 | `vurdertAv` som `Bruker` eller `String` | `Bruker`. Se F12. |
| Q8 | Trengs egen lagring av brevbekreftelsen? | Nei, den lagres på avklaringsbehovet. Se F13. |
| Q9 | Skal `kopier` kopiere grunnlaget? | Nei, hver søknad gir en ny behandling uten `forrigeBehandling`. |
| Q10 | Skal `NO` godtas som land? | Nei. Se F15. |
| Q11 | Skal papirsøknader om AAP i utlandet journalføres maskinelt? | Ja. Søknaden trenger ikke strukturert data, så tidsbesparelsen veier tyngre enn risikoen. Se F16. |

## Tiltak som gjelder flere funn

Dette er forslag. Ingenting av dette er gjort ennå.

| Prioritet | Tiltak | Forebygger |
|---|---|---|
| 1 | Flytt arbeidsprosessen `/spesifiser`, `/implementer` og `/ferdigstill` med spesifikasjonsmal fra behandlingsflyt til postmottak. Postmottak har bare oppskrifts-skills, og spesifikasjonen ble skrevet uten mal. | F1, F6 |
| 2 | Legg til en tabell over kontraktfelt (produsent og forbruker) i spesifikasjonsmalen. | F1 |
| 3 | Legg til sjekk for ubrukte kontraktfelt i `flyt-review`. | F1 |
| 4 | Legg til sjekk av fjernede linjer etter konfliktløsning, og sjekk av duplikate Flyway-versjoner, i `verifiser`. | F2, F3 |
| 5 | Legg til en sjekkliste for nye avklaringsbehov (rolle, totrinn, kvalitetssikring) og nye steg (hvilke steg vises på samme side) i `/spesifiser`, og de samme spørsmålene i `nytt-avklaringsbehov` og `nytt-steg`. | F4, F5 |
| 6 | Legg til en tabell over manuelle punkter og avhengigheter mellom steg i spesifikasjonsmalen, og et spørsmål i `/spesifiser` om hvorfor et menneske skal inn. | F1, F6 |
| 7 | Lag skillen `ny-innsendingstype` med sjekkliste for valgene som kompilatoren tvinger frem (prioritet, årsak, vurderingsbehov, mappinger). | F8 |
| 8 | Legg til en tilstandsoversikt (status og avslutning per steg) i spesifikasjonsmalen, og spørsmålet «kan en vurdering endres senere?» i `/spesifiser` og `nytt-steg`. | F9 |
| 9 | Lag skillen `nytt-grunnlag-api` med `vurderingerMeta` som kanonisk mønster, og legg regelen «sammenlign minst tre eksempler» i agentinstruksjonene. | F10 |
| 10 | Skriv i `nytt-avklaringsbehov` og `nytt-steg` at vurderingsskjemaer skal ha mellomlagring som standard, og at agenten bare spør ved enkle ja/nei-skjemaer. | F11 |
| 11 | Legg til regelen «bruk domeneobjekter (`Bruker`, `Periode`, `BehandlingId`), ikke primitive typer» i agentinstruksjonene, og spørsmålet «finnes dette allerede på avklaringsbehovet?» i `/spesifiser` og `nytt-avklaringsbehov`. | F12, F13 |
| 12 | Legg til en egen linje for API-tester i teststrategien i spesifikasjonsmalen, og et API-testeksempel i `nytt-grunnlag-api`. Eksisterende dårlig dekning er ikke et argument for å hoppe over testen. | F14 |
| 13 | Legg til en kolonne for gyldige og ugyldige verdier per inputfelt i spesifikasjonsmalen, og en sjekk i `flyt-review` av at frontendens begrensninger også finnes i backend-valideringen. | F15 |
| 14 | Lag en skill for nye dokumenttyper i postmottak med spørsmålene «trenger vi strukturert data?», «kan det komme på papir?» og avveiningen mellom tidsbesparelse og risiko (uleselig skanning, flere personers dokumenter i samme journalpost). Agenten skal be utvikleren bekrefte valget for papir. Legg også til regelen «navn skal beskrive hva som sjekkes» og «skriv motsigelser mot tidligere beslutninger som spørsmål før koden endres». | F16 |

Spesifikasjonen dekker seks repoer, men ligger i postmottak. Ved neste oppgave av denne typen bør den ha en egen
seksjon som lister hvert repo og hva som må endres der.

## Slik oppdateres rapporten

For hver ny tilbakemelding legges det til en seksjon `F<nr>` med tilbakemelding, hva som skjedde, årsak, hvordan det
kunne vært forhindret og status. Oversiktstabellen og fremdriftstabellen oppdateres samtidig. Spørsmål som blir
besvart flyttes fra «Åpne spørsmål» til en egen seksjon, med svaret og konsekvensen.
