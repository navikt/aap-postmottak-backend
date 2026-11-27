# Oppskrifter: utvide motoren trygt

Stier: `flyt/src/main/kotlin/no/nav/aap/postmottak/` med mindre noe annet er oppgitt. Hver oppskrift slutter med hva du bør teste. Utrullinger er rullerende, og behandlinger, jobber og steg_historikk-rader overlever utrullinger. Spør derfor alltid «hva skjer med behandlinger som er under arbeid?».

## Legg til et steg i en eksisterende flyt

1. Hvis du trenger en ny `StegType`, legg den til i `kontrakt/.../steg/StegType.kt` med en `StegGruppe` og behandlings-`Status` den innebærer. Navnet lagres (`steg_historikk.steg`, `avklaringsbehov.funnet_i_steg`), så et nytt navn krever migrering (presedens: `V1.10`, `V1.11`).
2. Ny `StegGruppe` med `skalVises = true` og `obligatoriskVisning = false`: legg til en `StegGruppeVisningUtleder`-implementasjon i `flyt/flate/visning/` med én `RepositoryProvider`-konstruktør. `DynamiskStegGruppeVisningService` finner den med refleksjon. Uten en slik implementasjon kaster `GET /api/behandling/{ref}/flyt` exception.
3. Implementer steget i `forretningsflyt/steg/<flyt>/`:
   ```kotlin
   class MittSteg(private val repo: MinRepository, private val gateway: MinGateway) : BehandlingSteg {
       companion object : FlytSteg {
           override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg =
               MittSteg(repositoryProvider.provide(), gatewayProvider.provide())
           override fun type() = StegType.MITT_STEG
       }

       override fun utfør(kontekst: FlytKontekst): StegResultat {
           if (repo.hentVurdering(kontekst.behandlingId) != null) return Fullført   // idempotent omkjøring
           // avgjør automatisk, eller:
           return FantAvklaringsbehov(Definisjon.MITT_BEHOV)
       }
   }
   ```
   La konstruktøren til både steget og tjenestene det bruker ta inn de konkrete avhengighetene.
   Slå opp repositorier og gatewayer i `companion object` sin `konstruer`, og bygg tjenester med deres egen `konstruer`.
   Ikke send `RepositoryProvider` eller `GatewayProvider` til konstruktøren for å gjøre oppslag i forretningslogikken.
   `konstruer` kjører én gang per fase (fem ganger per gjennomkjøring): ingen I/O der.
4. Sett inn `.medSteg(steg = MittSteg)` på riktig plass i `forretningsflyt/behandlingstyper/<Flyt>.kt`.
5. Behandlinger under arbeid: de som allerede er forbi den nye plasseringen, kjører aldri steget (med mindre de tilbakeføres). Å fjerne et steg ødelegger behandlinger der det fjernede steget er aktivt (`Steg X finnes ikke i flyten`). Flytt dem med en migrering (presedens: `V1.37__fjern_kategoriseringssteg.sql`).
6. Test: enhetstest av steget med fakes, og ett `Flyttest`-scenario gjennom hele flyten (se `postmottak-testing`).

## Legg til et avklaringsbehov

1. `kontrakt`: legg koden til i `enum AvklaringsbehovKode` og en `const val MITT_BEHOV_KODE`, deretter en `Definisjon`-oppføring (`type`, `løsesISteg`, `defaultFrist` for ventepunkter, `løsesAv`). Companion-objektet til `Definisjon` validerer unike koder og ventepunkt-regler når klassen lastes.
2. Løsning i pakken `avklaringsbehov.løsning` (interfacet er sealed, så klassen må ligge i den pakken og modulen):
   ```kotlin
   @JsonIgnoreProperties(ignoreUnknown = true)
   @JsonTypeName(value = MITT_BEHOV_KODE)
   class MittBehovLøsning(
       val beslutning: Boolean,
       @param:JsonProperty("behovstype", required = true, defaultValue = MITT_BEHOV_KODE)
       val behovstype: String = MITT_BEHOV_KODE,
   ) : AvklaringsbehovLøsning {
       override fun løs(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, kontekst: AvklaringsbehovKontekst) =
           MittBehovLøser.konstruer(repositoryProvider, gatewayProvider).løs(kontekst, this)
   }
   ```
   Jackson-registreringen skjer automatisk (`utledSubtypes()` i `App.kt`).
3. Løser i pakken `avklaringsbehov.løser` som implementerer det sealed `AvklaringsbehovsLøser<MittBehovLøsning>`, med `forBehov()` og `companion object : LøserKonstruktør<...>`. Valider løsningen først og kast `UgyldigForespørselException` (HTTP 400, meldingen vises til saksbehandler) ved ugyldig input. Bruk aldri `require` eller en `init`-blokk i løsningen (det blir 500 med «En ukjent feil oppsto»). Lagre deretter en vurdering og returner `LøsningsResultat(begrunnelse)`. Legg klassen til i det forventede settet i `AvklaringsbehovsLøserTest`.
4. Steget oppretter behovet og leser vurderingen ved neste kjøring.
5. Legg til et lese-endepunkt (`api/.../faktagrunnlag/...`) hvis frontend trenger grunnlag. `openapi.json` genereres i CI (`./gradlew :app:genererOpenApiJson`). Ikke rediger den for hånd.
6. Koordiner utrullingene: `Definisjon` serialiseres i `DokumentflytStoppetHendelse`, så aap-oppgave og aap-statistikk må godta den nye koden (ny kontraktversjon) før den kan dukke opp. Frontend (aap-saksbehandling) trenger et brukergrensesnitt.
7. Test løseren og steget hver for seg. Kall `utfør` med repositorier i minnet og fakes, uten mocks eller database (se `postmottak-testing`, mal 1). En flyttest som løser behovet på tvers av steg, kan bruke ekte Postgres.

## Legg til et informasjonskrav

Hold saksbehandlerens svar og resultatet fra informasjonskravet i separate tabeller. For eksempel lagrer `AvklarUnder18SøknadLøser` alderssvaret i `under18_soknadsavklaring`. `Under18FødselsdatoInformasjonskrav` leser svaret og er alene om å skrive til `under18_aldersgrunnlag`.

1. Implementer `Informasjonskrav<Input, Registerdata>`. Input implementerer `InformasjonskravInput`, registerdata implementerer `InformasjonskravRegisterdata`. Bruk `IngenInput` eller `IngenRegisterData` når en fase ikke trenger data. `klargjør(kontekst)` leser input fra databasen, `hentData(input)` henter eksterne opplysninger uten databasetilgang, og `oppdater(input, registerdata, kontekst): Endret` oppdaterer databasen uten eksterne kall.
2. Legg til et stabilt navn i `InformasjonskravNavn` og et `companion object : Informasjonskravkonstruktør` med `navn` og eventuelle `avhengigheter`. Ikke gi lagrede navn nye verdier. Injiser repositories i konstruktøren. `InformasjonskravGrunnlagImpl` er et avtalt unntak: historikkrepositoryet slås opp én gang i en `private val` i klassen, ikke i metodene.
3. Knytt det til flyten: `.medSteg(steg = X, informasjonskrav = listOf(MittInformasjonskrav))`. Det kjører da i OPPDATER_FAKTAGRUNNLAG-fasen til X og, ved hver gjenopptakelse der markøren står etter X, i `forberedBehandling`. Hele livssyklusen kjøres sekvensielt per informasjonskrav, slik at neste `klargjør` ser oppdateringene fra avhengighetene.
4. `InformasjonskravGrunnlagImpl` registrerer kjøringene automatisk når hele listen er ferdig, også ved `IKKE_ENDRET`. `INFORMASJONSKRAV_OPPDATERT` lagrer journalpost, behandling, navn, tidspunkt og **bare input** som JSONB, aldri registerdata. `InformasjonskravRepository.hentOppdateringer` finner siste kjøring per navn for journalposten, på tvers av behandlinger. `forrigeInput<Input>()` leser typet input; eldre historikk uten input gir `null`. Historikken endrer ikke når informasjonskrav kjøres. Ikke legg til egen `slett`-implementasjon for historikkrepositoryet.
5. **Semantikk for tilbakeføring:** å returnere `ENDRET` tilbakefører hver gjenopptatte behandling forbi X til X (se `engine-walkthrough.md` §3). Returner `ENDRET` bare for endringer som krever at X og senere steg vurderes på nytt, og sørg for at lagrede og hentede data er like når ingenting har endret seg. Dekk det med en flyttest som gjenopptar etter X. Bruk hele ordet «informasjonskrav» i nye navn og tekster; «krav» er et eget begrep i behandlingsflyt.

## Legg til en vurdering per behandling (grunnlag-repository)

1. Interface ved siden av domenekoden, som utvider **`no.nav.aap.lookup.repository.Repository`** (den lokale markøren med `kopier`/`slett`), ikke komponenters `Repository`.
2. Implementasjon i `repository/src/main/kotlin/.../repository/` med `companion object : Factory<Impl>` og grunnlag-mønsteret som alle vurderinger bruker:
   - `MIN_VURDERING` (uforanderlige rader) + `MIN_VURDERING_GRUNNLAG(BEHANDLING_ID, MIN_VURDERING_ID, AKTIV)`.
   - lagre: sett inn vurdering → `UPDATE ..._GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV` → sett inn aktivt grunnlag.
   - hent: join på `AKTIV`.
   - `kopier(fra, til)`: sett inn grunnlag-rader for `til` som peker på de samme vurderings-IDene. Overstyr den hvis vurderingen må følge med fra Journalføring → DokumentHåndtering (`GrunnlagKopierer` i `VideresendSteg`).
3. Registrer i `repository/.../PostgresRepositoryRegistry.kt`. Slå den opp med `repositoryProvider.provide<Interface>()`. Registeret matcher bare implementasjonens direkte superinterface.
4. Flyway-migrering `repository/src/main/resources/flyway/V1.<neste>__<beskrivelse>.sql` (list mappa for å finne høyeste nummer). Hver tabell trenger en primærnøkkel: prod har en logisk replikeringspublisering `FOR ALL TABLES` (datastream, `V1.51`), og Postgres avviser UPDATE/DELETE på publiserte tabeller uten replica identity. Tester kjører uten den publiseringen og fanger det ikke opp.
5. Test: repositorytest mot `TestDataSource` (lagre, hent, kopier).

## Legg til en flyt (TypeBehandling)

1. `kontrakt/.../behandling/TypeBehandling.kt` (med en identifikator). Serialiseres i hendelser, så mottakerne må kjenne den først.
2. `object MinFlyt : BehandlingType` i `forretningsflyt/behandlingstyper/`: START_BEHANDLING først, `AvsluttBehandlingSteg` (IVERKSETTES) sist, etter konvensjon.
3. Koble den i `utledType` (`flyt/BehandlingType.kt`; `when` er uttømmende).
4. Den som oppretter den: `behandlingRepository.opprettBehandling(journalpostId, type)`, eventuelt `GrunnlagKopierer.overfør`, deretter legge `ProsesserBehandlingJobbUtfører` i køen med `forBehandling(journalpostId, behandlingId)`.

## Legg til en jobb

1. `class MinJobbUtfører(...) : JobbUtfører` med `companion object : ProvidersJobbSpesifikasjon { konstruer(repositoryProvider, gatewayProvider); type; navn; beskrivelse; retries?; cron? }`.
2. Registrer den i `prosessering/ProsesseringsJobber.alle()`, ellers kan ikke motoren parse lagrede jobber av den typen.
3. Grupper den: `forBehandling(journalpostId, behandlingId)` eller `forSak(journalpostId)` for å kjøre i rekkefølge per journalpost/behandling. Ingen av delene for uavhengige jobber eller cron-jobber.
4. `type` lagres: endre den aldri, og behold en stubb etter at funksjonen er fjernet. Hold payload-klasser bakoverkompatible.
5. Test utføreren direkte med fakes. Legg til en motorbasert test bare hvis rekkefølge eller retries betyr noe.

## Legg til en integrasjon (gateway)

1. Interface i `gateway/` (eller ved siden av domenet) som utvider `no.nav.aap.komponenter.gateway.Gateway`.
2. Implementasjon i `klienter/` med `companion object : Factory<Impl>` (vanligvis en singleton som opprettes lazy) som leser `requiredConfigForKey("INTEGRASJON_..._URL")`.
3. Registrer i `klienter/.../DefaultGatewayProvider.kt`. Legg til miljøvariabler og utgående tilgang i `.nais/dev-gcp.yaml` og `.nais/prod-gcp.yaml`.
4. Legg til en fake (se `postmottak-testing/references/fakes.md`): en HTTP-rute i `lib-test/.../FakeServers.kt` pluss system property, eller en fake i koden. Å registrere en annen implementasjon av samme Gateway-interface erstatter den tidligere. Slik bytter tester inn fakes.

## Legg til et API-endepunkt

1. I `api/`, som `fun NormalOpenAPIRoute.mittApi(dataSource, repositoryRegistry, ...)`, registrert i `App.kt` inne i `apiRouting { }`.
2. Bruk `authorizedGet` / `authorizedPost` fra tilgang-pluginen. For ruter med behandlingsomfang bruker du `JournalpostPathParam("referanse", journalpostIdFraBehandlingResolver(...))`. Mutasjoner trenger `Operasjon.SAKSBEHANDLE`, drift-endepunkter `DRIFTE` / `DRIFT_LES`. Vanlig `get`/`post` (med `@Suppress("UnauthorizedGet")`) er bare for lokale ruter og testruter.
3. Når du muterer en behandling: ta låsen (`TaSkriveLåsRepository.lås`), kjør `BehandlingTilstandValidator`, muter, og kjør deretter `verifiserSkrivelås`. Kjør aldri steg i forespørselen. Legg `ProsesserBehandlingJobbUtfører` i køen.
4. Endepunkter som bare leser: `dataSource.transaction(readOnly = true)`.
