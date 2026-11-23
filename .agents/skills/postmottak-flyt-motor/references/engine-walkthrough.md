# Gjennomgang av motoren: FlytOrkestrator, StegOrkestrator, BehandlingFlyt

Alle stier ligger under `flyt/src/main/kotlin/no/nav/aap/postmottak/` med mindre noe annet er oppgitt. Påstander merket **verifisert** er bekreftet ved å kjøre tester og lese motor-tracen.

## 1. Hvem starter prosesseringen

Hver kjøring av steg starter med en `ProsesserBehandlingJobbUtfører`-jobb. Dette er alle kallestedene i produksjon som legger den i køen:

| Kallested | Når |
| --- | --- |
| `mottak/VurderRelevantDokumentForAAPJobbUtfører` | Ny relevant journalpost: oppretter `Fordeling`-behandlingen |
| `forretningsflyt/steg/fordeling/FordelingVideresendSteg` | Fordeling bestemte KELVIN: oppretter `Journalføring`-behandlingen |
| `forretningsflyt/steg/journalføring/VideresendSteg` | Journalført til en Kelvin-sak: oppretter `DokumentHåndtering`-behandlingen |
| `mottak/KafkaStreams.kt` (`JoarkKafkaHandler`) | Ny AAP-hendelse eller temaendring for en journalpost med åpen Journalføring-behandling, eller AAP-retur (oppretter en ny Journalføring-behandling direkte, uten Fordeling) |
| `avklaringsbehov/AvklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering` | Etter hver løsning fra API-et (`POST /api/behandling/løs-behov`) |
| `prosessering/GjenopptaBehandlingJobbUtfører` | Cron 07:00: behandlinger i `UTREDES` med et åpent behov der fristen er passert |
| `api/.../BehandlingApi` `GET /api/behandling/{ref}/forbered` | Frontend åpner en inaktiv behandling (ingen aktivitet på 15 minutter, ikke avsluttet, ingen ventende jobb) |
| `api/.../DriftApi` `POST /api/drift/behandling/{ref}/prosesser` | Manuell ny trigging fra drift |

## 2. ProsesserBehandlingJobbUtfører

```kotlin
val skrivelås = låsRepository.lås(BehandlingId(input.behandlingId()))   // SELECT versjon FROM behandling WHERE id = ? FOR UPDATE
val kontekst = kontroller.opprettKontekst(skrivelås.id)                  // FlytKontekst(journalpostId, behandlingId, type)
kontroller.forberedOgProsesserBehandling(kontekst)
låsRepository.verifiserSkrivelås(skrivelås)                             // UPDATE ... SET versjon = versjon+1 WHERE versjon = old
```

Radlåsen serialiserer prosesseringen mot API-mutasjoner (`løs-behov`, `sett-på-vent`, `forbered`) som tar den samme låsen. Økningen av versjon ugyldiggjør alle `behandlingVersjon`-verdier frontend sitter med.

## 3. FlytOrkestrator.forberedBehandling

```text
behandling, avklaringsbehovene = last
avklaringsbehovene.validateTilstand(behandling)          kaster UgyldigForespørsel hvis status == AVSLUTTET
flyt = utledType(type).flyt(); flyt.forberedFlyt(behandling.aktivtSteg())   markør := gjeldende steg

if på vent:                                              et åpent VENTEPUNKT
    løs hvert ventepunkt med frist <= i dag             som SYSTEMBRUKER
    if ikke lenger på vent:
        tilbakefør(flyt.tilbakeflyt(expired))           tilbake til steget ventepunktet hører til (ingen effekt hvis samme steg)

førTilbakeTilTidligsteÅpneAvklaringsbehov               åpent behov i et TIDLIGERE steg enn det gjeldende?
                                                         log.error("...er dette en bug") og tilbakefør dit

endret = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(flyt.alleFaktagrunnlagFørGjeldendeSteg())
tilbakefør(flyt.tilbakeflytEtterEndringer(endret))
```

### Informasjonskrav-tilbakeføringen i detalj

- `alleFaktagrunnlagFørGjeldendeSteg()` returnerer `(steg, krav)` for hvert steg strengt før det gjeldende. Flagget `oppdaterFaktagrunnlag` er alltid `true` (standard i builderen, aldri endret).
- `InformasjonskravGrunnlagImpl.oppdaterFaktagrunnlagForKravliste` kaller `oppdater()` på hvert krav og returnerer kravene der resultatet var `ENDRET`, slik KDoc på `InformasjonskravGrunnlag` sier og slik det er i aap-behandlingsflyt.
- `tilbakeflytEtterEndringer(list)` velger det tidligste steget der kravlisten inneholder et av de returnerte kravene (unntatt steg med status `OPPRETTET`, altså START_BEHANDLING), og tilbakefører dit hvis det er før gjeldende steg. En tilbakeføring logger `Tilbakeført etter oppdatering av registeropplysninger fra 'X' til 'Y'` og sender en ekstra `DokumentflytStoppetHendelse`.
- `JournalpostInformasjonskrav` returnerer `ENDRET` bare når den lagrede journalposten skiller seg fra SAF og forskjellen i tema eller saksnummer er relevant. `SaksnummerInfoKrav` returnerer alltid `IKKE_ENDRET`. En vanlig Journalføring-behandling tilbakeføres derfor ikke når den gjenopptas. Lagrede og hentede data må være like, ellers melder informasjonskravet `ENDRET` ved hver gjenopptakelse.
- Historikk: commit `40cf556e7` (2024-09) erstattet `harIkkeGjortOppdateringNå` med `oppdater() == ENDRET`, men beholdt `filterNot`. Det snudde meningen fram til november 2026. I den perioden ble Journalføring tilbakeført til AVKLAR_TEMA ved nesten hver gjenopptakelse, noe gammel steghistorikk viser som `AVKLAR_SAK:SOUAT > AVKLAR_TEMA:TSOUAX`.
- Selv med den «tilsiktede» semantikken ville tilbakeføringen som regel skjedd: `JournalpostInformasjonskrav` melder `ENDRET` når Joarks saksnummer/tema avviker fra vurderingen, som er normaltilstanden fram til SETT_FAGSAK har kjørt.
- Fordeling og DokumentHåndtering påvirkes ikke: ingen krav før de manuelle stegene.

### tilbakefør

```text
if flyt er tom: return
steg = tilbakeflyt.aktivtSteg()                 gjeldende steg (første element i den reverserte delflyten)
loop:
    stegOrkestrator.utførTilbakefør(steg)      logger StegTilstand(steg, TILBAKEFØRT). Ingen stegkode kjører.
    steg = tilbakeflyt.neste()                 forrige steg; flytter også markøren i foreldreflyten
    if steg == null:
        loggStopp(); if harHeltStoppet: behandlingHendelseService.stoppet(...)   → flyt.hendelse-jobb
        return
```

`harHeltStoppet = false` bare når den kalles fra `forberedLøsingAvBehov` (API-stien). Fra `forberedBehandling` sender den alltid en hendelse, selv om `prosesserBehandling` fortsetter umiddelbart.

## 4. FlytOrkestrator.prosesserBehandling

```text
behandling, avklaringsbehovene = last på nytt; validateTilstand
if på vent: løs utløpte ventepunkter (ingen tilbakeføring her)
flyt = ny flyt; gjeldende = flyt.forberedFlyt(behandling.aktivtSteg())
loop:
    result = stegOrkestrator.utfør(gjeldende, kontekst, behandling, flyt.faktagrunnlagForGjeldendeSteg())
    validerPlassering(flyt, åpne behov)        IllegalStateException hvis et åpent behov hører til et tidligere steg
    neste = flyt.neste()                       flytter markøren, selv når vi snart stopper
    if !result.kanFortsette() || neste == null:
        if neste == null:
            status = AVSLUTTET
            check(ingen åpne behov)            "Behandlingen er avsluttet, men det finnes åpne avklaringsbehov."
        behandlingHendelseService.stoppet(ny behandling)      → StoppetHendelseJobbUtfører (oppgave + statistikk)
        return
    gjeldende = neste
```

`behandling` er ett objekt i minnet som deles med `StegOrkestrator`. `loggStegHistorikk` oppdaterer det via `behandling.visit(...)`, så `behandling.aktivtSteg()` er i synk gjennom hele jobben.

## 5. StegOrkestrator.utfør: fasene i ett steg

`StegStatus.rekkefølge()` = START, OPPDATER_FAKTAGRUNNLAG, UTFØRER, AVKLARINGSPUNKT, AVSLUTTER. For hver fase: konstruer steget (`FlytSteg.konstruer`), kjør fasen, logg en `StegTilstand(steg, fase)`-rad.

| Fase | Arbeid | Etterpå |
| --- | --- | --- |
| START | ingen | rad, **savepoint** |
| OPPDATER_FAKTAGRUNNLAG | `oppdater()` på stegets egne krav. Resultatet ignoreres her. | rad, **savepoint** |
| UTFØRER | `steg.utfør(kontekst)`, deretter håndtering av overgangen (under) | rad (skrives ikke hvis `utfør` kaster) |
| AVKLARINGSPUNKT | åpent behov med `skalLøsesISteg(dette steget)`? → `Stopp`, returner | rad |
| AVSLUTTER | ingen, returnerer `Fortsett` | rad |

Håndtering av resultatet fra UTFØRER:

| Steget returnerte | Overgang | Handling i motoren |
| --- | --- | --- |
| `Fullført` | `Fortsett` | Hvis behandlingen **ikke** har noe avklaringsbehov som ikke er vent (uansett status), løses alle åpne ventepunkter («Har gått videre til nytt steg»). |
| `FantAvklaringsbehov(def)` | `FunnetAvklaringsbehov` (`kanFortsette() = true`) | `avklaringsbehovene.leggTil(def, dette steget)`: opprett, gjenåpne hvis lukket, ignorer hvis allerede åpent. |
| (ingen) | `FunnetVentebehov` | Død gren: ingen `StegResultat` produserer den. `FantAvklaringsbehov` avviser ventepunkt-definisjoner med `require`. |

`utførTilbakefør` logger bare `TILBAKEFØRT` (status for det stegets `StegType`), uten andre sideeffekter enn `konstruer`.

### Logging av en StegTilstand

`StegOrkestrator.loggStegHistorikk` → `behandling.visit(tilstand)` (deaktiverer forrige aktive oppføring i minnet, oppdaterer status i minnet) → `BehandlingRepository.loggBesøktSteg` (`UPDATE steg_historikk SET aktiv = false ...` + innsetting av en ny aktiv rad; den unike indeksen tillater én aktiv rad per behandling) → `oppdaterBehandlingStatus` hvis `StegType.status` er en annen.

Konsekvenser:
- `Behandling.aktivtSteg()` = steget i den aktive raden; `START_BEHANDLING` hvis det ikke finnes historikk ennå.
- En stoppet behandling har aktiv rad `(steg, AVKLARINGSPUNKT)`; en som har feilet, har typisk `(steg, OPPDATER_FAKTAGRUNNLAG)`.
- Omtrent fem rader per steg per gjennomkjøring. Tilbakeføringer og gjenopptakelser multipliserer dette.

## 6. Behandlingsstatus

| StegType | Status mens markøren står der |
| --- | --- |
| START_BEHANDLING | OPPRETTET |
| AVKLAR_TEMA, AVKLAR_SAK, SETT_FAGSAK, ENDELIG_JOURNALFØRING, AVKLAR_FORDELING, DIGITALISER_DOKUMENT, OVERLEVER_TIL_FAGSYSTEM | UTREDES |
| VIDERESEND, AVKLAR_FORDELING_VIDERESEND, IVERKSETTES | IVERKSETTES |
| etter siste steg | AVSLUTTET (settes eksplisitt av FlytOrkestrator) |

`Status.erAvsluttet()` returnerer sann for IVERKSETTES **og** AVSLUTTET (brukes for skrivebeskyttet frontend og `/forbered`). `ValiderBehandlingTilstand` blokkerer bare AVSLUTTET. Å være på vent endrer ikke status.

## 7. Internt i BehandlingFlyt

- `BehandlingType.flyt()` bygger en **ny** `BehandlingFlyt` ved hvert kall. Den har en uforanderlig liste med `Behandlingsflytsteg(steg, kravliste, oppdaterFaktagrunnlag)` og en **muterbar markør** `aktivtSteg`. Aldri cache eller del en instans.
- `forberedFlyt(stegType)` setter markøren (kaster `IllegalArgumentException("Steg X finnes ikke i flyten")` for ukjente steg: det er dette som skjer med behandlinger under arbeid hvis et steg fjernes uten migrering).
- `neste()` flytter markøren framover og returnerer neste `FlytSteg`, eller `null` på slutten.
- `tilbakeflyt(...)` / `tilbakeflytEtterEndringer(...)` returnerer en barne-`BehandlingFlyt` som inneholder `[mål .. gjeldende]` reversert, med `parent = this`. Å kalle `neste()` på barnet flytter også markøren til foreldreflyten. En delflyt med størrelse ≤ 1 returneres som tom (ingenting å gjøre).
- `erStegFør(a, b)` sammenligner listeindekser; et steg som ikke finnes i flyten, har indeks -1 og er «før» alt.
- `Avklaringsbehov.løsesISteg()` oversetter `StegType.UDEFINERT` (MANUELT_SATT_PÅ_VENT) til `funnetISteg`.
- Rester fra kopien av behandlingsflyt, uten effekt i dag: `ÅrsakTilBehandling` / `kunRelevantVedÅrsakerHvisSattEllersIkke`, frivillige avklaringsbehov (ingen definisjon er `MANUELT_FRIVILLIG`, så `FrivilligeAvklaringsbehov` legger ikke til noe), `StegType.tekniskSteg` (alltid false), `kreverToTrinn`, `SENDT_TILBAKE_FRA_BESLUTTER`, `ÅrsakTilRetur`.

## 8. Kommentert reell trace (manuell Journalføring, deretter automatisk DokumentHåndtering)

Laget med `postmottak-testing/scripts/flyt-trace.sh` fra `Flyttest."Blir satt på vent for etterspørring av informasjon"` (forkortet):

```text
Motor: Plukket jobb [flyt.prosesserBehandling] ... behandlingId = 1            jobb 1 (testen la den i køen)
[b=1] StegOrkestrator: Behandler steg 'START_BEHANDLING'
[b=1] StegOrkestrator: Behandler steg 'AVKLAR_TEMA'
[b=1] JournalpostInformasjonskrav: Fant relevante endringer ...                 OPPDATER_FAKTAGRUNNLAG lagrer journalposten
[b=1] Avklaringsbehovene: Legger til avklaringsbehov :: AVKLAR_TEMA             opprettet via AvklaringsbehovService
[b=1] FlytOrkestrator: Resultat fra steg: Stopp. Aktivt steg: AVKLAR_TEMA.      AVKLARINGSPUNKT stoppet
[b=1] FlytOrkestrator: Neste utledet steg: AVKLAR_SAK.                          markøren flyttet likevel (ufarlig)
[b=1] JobbRepository: Planlagt kjøring av jobb[flyt.hendelse] ...               stoppet-hendelse til oppgave/statistikk
[b=-] AvklaringsbehovOrkestrator: Forsøker løse avklaringsbehov[AVKLAR_TEMA]   API-/testtråd: løsning
[b=-] AvklaringsbehovOrkestrator: Tar av vent for behandling ...                AvklarTemaLøser → taAvVentPgaGosys → ekstra hendelse
[b=-] JobbRepository: Planlagt kjøring av jobb[flyt.prosesserBehandling] ...    jobb for gjenopptakelse
[b=1] StegOrkestrator: Behandler steg 'AVKLAR_TEMA'                             kjører steget på nytt; vurderingen finnes nå
[b=1] StegOrkestrator: Behandler steg 'AVKLAR_SAK'
[b=1] StegOrkestrator: Fant avklaringsbehov: AVKLAR_SAK(kode='1340')            eksplisitt FantAvklaringsbehov
[b=1] FlytOrkestrator: Stopper opp ved AVKLAR_SAK ...
[b=-] Avklaringsbehovene: Løser avklaringsbehov[AVKLAR_SAK] ...
[b=1] FlytOrkestrator: Tilbakeført ... fra 'AVKLAR_SAK' til 'AVKLAR_TEMA'       informasjonskrav-tilbakeføring (avsnitt 3)
[b=1] StegOrkestrator: Behandler steg 'AVKLAR_TEMA' ... 'AVKLAR_SAK' ... 'SETT_FAGSAK' ... 'ENDELIG_JOURNALFØRING' ... 'VIDERESEND' ... 'IVERKSETTES'
[b=1] FlytOrkestrator: Behandlingen har nådd slutten, avslutter behandling
[b=2] StegOrkestrator: Behandler steg 'START_BEHANDLING'                        DokumentHåndtering opprettet av VideresendSteg
[b=2] StegOrkestrator: Fant avklaringsbehov: DIGITALISER_DOKUMENT(kode='1338')
```
