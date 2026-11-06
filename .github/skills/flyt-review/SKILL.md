---
name: flyt-review
description: Sjekkliste-gjennomgang av en diff som rører flyt, steg, avklaringsbehov, fordelingsregler, jobber, klienter, kontrakt eller migrasjoner. Bruk på egen diff før PR, eller på en PR som skal reviewes.
---
# Gjennomgang av endringer i postmottak

Gå gjennom diffen (`git --no-pager diff origin/main...HEAD` eller PR-nummeret) punkt for punkt og svar ja/nei/ikke
relevant med filreferanse. Ikke godkjenn på magefølelse; finn beviset i diffen.

**Persondata**
- Logges fødselsnummer, `Ident.identifikator`, `Person`, respons-objekter fra PDL/SAF/Gosys eller dokumentinnhold?
  (Skal være nei; `journalpostId` er greit.) Inneholder tester eller fakes noe som ser ut som ekte data?

**Lagrede navn og kontrakt**
- Er en eksisterende verdi i `Definisjon`, `AvklaringsbehovKode`, `StegType`, `TypeBehandling`, `Status`, en jobb-`type`,
  en lagret enum, et regelklassenavn eller et `PostmottakFeature`-navn endret, fjernet eller omdøpt? (Skal være nei.)
  Er en ny toggle stavet slik den skal hete i Unleash?
- Rører PR-en `kontrakt/`? Er `AvklaringsbehovKode`-konstant, enum og `Definisjon` endret samlet? Står det i
  PR-beskrivelsen hvem som må bumpe (aap-oppgave, aap-statistikk, aap-tilgang)?
- Endres request/respons i `api/`? Er `openapi.json` oppdatert, og er frontend nevnt?

**Idempotens og gjenkjøring**
- Kan steget, jobben eller løseren kjøres to ganger uten duplikater eller doble kall mot Joark/Gosys/Arena? Starter
  steget med «vurdering finnes → `Fullført`», eller avstemmer det behovet via `AvklaringsbehovService`? Finnes en test
  som kjører den to ganger?
- Endres stegrekkefølgen eller en flytdefinisjon? Hva skjer med behandlinger som står i flyten nå?

**Fordeling**
- Ny eller endret regel i `fordeler/regler/`: blir den et krav for Kelvin (standard), og er det meningen? Er
  `erAktiv` riktig per miljø? Er `RegelResultatTest` oppdatert?

**Data**
- `UPDATE` mot tabeller med `aktiv` utenom deaktivering? Ny migrasjon rullende-trygg (nullable/`DEFAULT`, ingen
  `DROP`/`RENAME` sammen med kodeendring)? Kommer migrasjonen med repository-endring og test i samme PR?
- Nytt grunnlag som skal følge til `DokumentHåndtering`: har repositoryet `kopier()`?

**Registreringer og integrasjoner**
- Nye klasser registrert i `PostgresRepositoryRegistry`, `DefaultGatewayProvider`, `ProsesseringsJobber`, en
  flytdefinisjon, `FakeUnleash`? (`:arkitektur:test` sjekker de fleste.)
- Ny klient: `.nais/dev-gcp.yaml` + `prod-gcp.yaml` (outbound, miljøvariabler) og fake i `FakeServers`?
- Toggle: sjekkes den før dyre kall? Hva skjer når Unleash er nede?

**Tester og PR**
- Er det test på riktig nivå for den nye grenen (steg-, løser-, regel-, jobb- eller klienttest), ikke bare `Flyttest`?
  Gjelder det `fordeler/`, PDL, Norg eller Gosys (områdene med mest fiks-commits), er feilgrenen testet?
- Stemmer KDoc og kommentarer med koden? `null` og tom liste behandlet likt der de skal?
- Sier PR-beskrivelsen hva som er intensjonen og hvordan det er testet, og om DB-/Kafka-testene kjørte?

Avslutt med en kort liste over funn sortert etter alvorlighet, hver med fil:linje.
