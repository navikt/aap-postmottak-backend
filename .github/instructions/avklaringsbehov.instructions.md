---
applyTo: "flyt/src/main/kotlin/no/nav/aap/postmottak/avklaringsbehov/**,kontrakt/src/main/kotlin/no/nav/aap/postmottak/kontrakt/avklaringsbehov/**"
description: "Definisjon, AvklaringsbehovKode, løsninger og løsere: publisert kontrakt, konstant og enum i synk, hva løseren gjør"
---
# Regler for avklaringsbehov, løsere og løsninger

- `Definisjon` og `AvklaringsbehovKode` er publisert kontrakt og lagret i `AVKLARINGSBEHOV.definisjon`. Eksisterende
  verdier endres aldri. `1337` står fortsatt i enumen selv om `Definisjon` for den er borte; gjør det samme:
  deprecate, ikke slett. (`1342`/`VENT_PA_GOSYS` er i bruk, men mangler `const val`; kopier ikke det.)
- Nytt behov endrer tre ting samlet i `kontrakt/`: `const val XXX_KODE = "NNNN"` og enum-verdien `` `NNNN` `` i
  `AvklaringsbehovKode.kt`, og en ny entry i `Definisjon.kt` (`type`, `løsesISteg`, `løsesAv`, evt. `defaultFrist`).
  `:arkitektur:test` sjekker at konstantene speiler enumen; `DefinisjonTest` (i `flyt/src/test`) sjekker at kodene er unike.
- `løsesAv` brukes av aap-tilgang til å avgjøre hvem som får løse behovet, og aap-oppgave lager oppgaver ut fra
  `Definisjon`. Nytt behov må inn hos dem (bump kontrakt) før flyten kan reise det i prod.
- Ny løsning i `løsning/`: `@JsonTypeName(XXX_KODE)`, `@JsonIgnoreProperties(ignoreUnknown = true)`, `behovstype` med
  `@param:JsonProperty("behovstype", required = true, defaultValue = XXX_KODE)`, implementerer `AvklaringsbehovLøsning`
  (sealed, så klassen må ligge i samme pakke), og `løs()` delegerer til løseren. Mal: `AvklarSaksnummerLøsning`.
- Ny løser i `løser/`: `class XxxLøser(avhengigheter) : AvklaringsbehovsLøser<XxxLøsning>` med
  `companion object : LøserKonstruktør<XxxLøsning>`; `forBehov()` må være unik. Legg klassen i `forventedeLøsere` i
  `AvklaringsbehovsLøserTest`, og registrer gateway-mocks der hvis konstruktøren trenger nye gateways.
- Løseren validerer, lagrer vurderingen og returnerer `LøsningsResultat`. Den endrer ikke avklaringsbehov-status, og
  det meste av arbeidet mot eksterne systemer gjør steget når flyten fortsetter i neste jobb. Unntak:
  `AvklarSakLøser` kaller `BehandlingsflytGateway.finnEllerOpprettSak` (et finn-eller-opprett-kall). Et nytt eksternt
  kall i en løser må tåle at saksbehandler sender løsningen på nytt.
- Ventepunkt (`BehovType.VENTEPUNKT`) settes via `AvklaringsbehovOrkestrator` og har frist; `GjenopptaBehandlingJobbUtfører`
  tar dem av vent. Bruk eksisterende `ÅrsakTilSettPåVent`-verdier eller legg til nye (lagres i DB).
- Test: `XxxLøserTest` med mockk (mal: `AvklarSakLøserTest`), stegtesten som reiser behovet, og `AvklaringsbehovLøsningTest`
  hvis JSON-formen endres.
