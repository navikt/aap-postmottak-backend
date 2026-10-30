---
applyTo: "kontrakt/**"
description: "kontrakt-modulen publiseres som bibliotek til aap-oppgave, aap-statistikk og aap-tilgang: bare legg til, aldri endre eller fjerne"
---
# Regler for kontrakt-modulen

- Alt her publiseres automatisk som `no.nav.aap.postmottak:kontrakt` til GitHub Packages: hver commit på main som
  rører `kontrakt/` gir ny patch-versjon (`deploy.yaml`, `bump_each_commit`), og `(MAJOR)` i commit-meldingen gir ny
  major. En «opprydding» her er en ny publisert versjon. Stopp og be om bekreftelse før du endrer noe.
- Konsumenter: aap-oppgave (`Status`, `DokumentflytStoppetHendelse`, `Definisjon`), aap-statistikk (hendelsene) og
  aap-tilgang (`Definisjon.løsesAv`). Frontendene (aap-saksbehandling, aap-paw-patrol) genererer typer fra OpenAPI,
  så enum-navn her er også frontend-kontrakt.
- Legg bare til; fjern aldri, omdøp aldri, endre aldri betydning. `@Deprecated` er verktøyet. Ny verdi tas i bruk i
  flyten først når konsumentene har bumpet versjonen.
- `AvklaringsbehovKode` har både `const val`-er og en enum som må speile hverandre; endre dem og `Definisjon` samlet.
- `TypeBehandling` og `Status` lagres i `BEHANDLING.type`/`.status`, `StegType` i `STEG_HISTORIKK`. Nytt navn her er
  en datamigrasjon.
- Ingen nye avhengigheter i `kontrakt/build.gradle.kts`. Tilgang-kontrakten er `compileOnly` (for `Rolle` i
  `Definisjon`), så konsumentene må ha den selv.
- `explicitApi` er bare `Warning`; skriv likevel `public` eksplisitt, som resten av modulen.
- Test: `./gradlew :kontrakt:test` (ingen Docker). Si i PR-beskrivelsen at kontrakten endres og hvem som må bumpe.
