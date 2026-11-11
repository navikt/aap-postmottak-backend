# Recipes: extending the engine safely

Paths: `flyt/src/main/kotlin/no/nav/aap/postmottak/` unless stated otherwise. Each recipe ends with what to test. Deployments are rolling and behandlinger, jobs and steg_historikk rows survive deploys, so always ask "what happens to in-flight behandlinger?".

## Add a step to an existing flyt

1. If a new `StegType` is needed, add it in `kontrakt/.../steg/StegType.kt` with a `StegGruppe` and the behandling `Status` it implies. The name is persisted (`steg_historikk.steg`, `avklaringsbehov.funnet_i_steg`); renaming needs a migration (precedents: `V1.10`, `V1.11`).
2. New `StegGruppe` with `skalVises = true` and `obligatoriskVisning = false`: add a `StegGruppeVisningUtleder` implementation in `flyt/flate/visning/` with a single `RepositoryProvider` constructor. `DynamiskStegGruppeVisningService` finds it by reflection; without one, `GET /api/behandling/{ref}/flyt` throws.
3. Implement the step in `forretningsflyt/steg/<flyt>/`:
   ```kotlin
   class MittSteg(private val repo: MinRepository, private val gateway: MinGateway) : BehandlingSteg {
       companion object : FlytSteg {
           override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg =
               MittSteg(repositoryProvider.provide(), gatewayProvider.provide())
           override fun type() = StegType.MITT_STEG
       }

       override fun utfør(kontekst: FlytKontekst): StegResultat {
           if (repo.hentVurdering(kontekst.behandlingId) != null) return Fullført   // idempotent re-run
           // decide automatically, or:
           return FantAvklaringsbehov(Definisjon.MITT_BEHOV)
       }
   }
   ```
   `konstruer` runs once per phase (five times per pass): no I/O there.
4. Insert `.medSteg(steg = MittSteg)` at the right position in `forretningsflyt/behandlingstyper/<Flyt>.kt`.
5. In-flight behandlinger: those already past the new position never run it (unless rewound). Removing a step breaks behandlinger whose active steg is the removed one (`Steg X finnes ikke i flyten`); move them with a migration (precedent: `V1.37__fjern_kategoriseringssteg.sql`).
6. Test: step unit test with fakes, and one `Flyttest` scenario through the whole flyt (see `postmottak-testing`).

## Add an avklaringsbehov

1. `kontrakt`: add the code to `enum AvklaringsbehovKode` and a `const val MITT_BEHOV_KODE`, then a `Definisjon` entry (`type`, `løsesISteg`, `defaultFrist` for ventepunkter, `løsesAv`). `Definisjon`'s companion validates unique codes and ventepunkt rules when the class loads.
2. Løsning in package `avklaringsbehov.løsning` (the interface is sealed, so the class must live in that package and module):
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
   Jackson registration is automatic (`utledSubtypes()` in `App.kt`).
3. Løser in package `avklaringsbehov.løser` implementing the sealed `AvklaringsbehovsLøser<MittBehovLøsning>`, with `forBehov()` and `companion object : LøserKonstruktør<...>`. Store a vurdering and return `LøsningsResultat(begrunnelse)`. Add the class to the expected set in `AvklaringsbehovsLøserTest`.
4. The step creates the behov and reads the vurdering on the next run.
5. Add a read endpoint (`api/.../faktagrunnlag/...`) if the frontend needs grunnlag. `openapi.json` is generated in CI (`./gradlew :app:genererOpenApiJson`); do not edit it by hand.
6. Coordinate releases: `Definisjon` is serialised in `DokumentflytStoppetHendelse`, so aap-oppgave and aap-statistikk must accept the new code (new kontrakt version) before it can appear; the frontend (aap-saksbehandling) needs a UI.
7. Test: løser test, step test, one flyt test that solves the behov.

## Add an informasjonskrav

1. Implement `Informasjonskrav.oppdater(kontekst): Endret` and a `companion object : Informasjonskravkonstruktør`.
2. Attach it: `.medSteg(steg = X, informasjonskrav = listOf(MittKrav))`. It then runs in X's OPPDATER_FAKTAGRUNNLAG phase and, on every resume where the cursor is after X, in `forberedBehandling`.
3. **Rewind semantics:** with the current `filterNot` in `InformasjonskravGrunnlagImpl`, a krav that returns `IKKE_ENDRET` makes every resumed behandling past X rewind to X (see `engine-walkthrough.md` §3). Decide deliberately and cover it with a flyt test that resumes after X.

## Add a per-behandling vurdering (grunnlag repository)

1. Interface next to the domain code, extending **`no.nav.aap.lookup.repository.Repository`** (the local marker with `kopier`/`slett`), not komponenter's `Repository`.
2. Implementation in `repository/src/main/kotlin/.../repository/` with `companion object : Factory<Impl>` and the grunnlag pattern used by all vurderinger:
   - `MIN_VURDERING` (immutable rows) + `MIN_VURDERING_GRUNNLAG(BEHANDLING_ID, MIN_VURDERING_ID, AKTIV)`.
   - lagre: insert vurdering → `UPDATE ..._GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV` → insert active grunnlag.
   - hent: join on `AKTIV`.
   - `kopier(fra, til)`: insert grunnlag rows for `til` pointing at the same vurdering ids. Override it if the vurdering must follow Journalføring → DokumentHåndtering (`GrunnlagKopierer` in `VideresendSteg`).
3. Register in `repository/.../PostgresRepositoryRegistry.kt`. Look it up with `repositoryProvider.provide<Interface>()`; the registry matches only the implementation's direct super-interface.
4. Flyway migration `repository/src/main/resources/flyway/V1.<next>__<beskrivelse>.sql` (list the folder for the highest number). Every table needs a primary key: prod has a logical-replication publication `FOR ALL TABLES` (datastream, `V1.51`), and Postgres rejects UPDATE/DELETE on published tables without a replica identity. Tests run without that publication and will not catch it.
5. Test: repository test against `TestDataSource` (lagre, hent, kopier).

## Add a flyt (TypeBehandling)

1. `kontrakt/.../behandling/TypeBehandling.kt` (with an identifikator). Serialised in hendelser, so consumers must know it first.
2. `object MinFlyt : BehandlingType` in `forretningsflyt/behandlingstyper/`: START_BEHANDLING first, `AvsluttBehandlingSteg` (IVERKSETTES) last by convention.
3. Map it in `utledType` (`flyt/BehandlingType.kt`; the `when` is exhaustive).
4. Whoever creates it: `behandlingRepository.opprettBehandling(journalpostId, type)`, optionally `GrunnlagKopierer.overfør`, then enqueue `ProsesserBehandlingJobbUtfører` with `forBehandling(journalpostId, behandlingId)`.

## Add a job

1. `class MinJobbUtfører(...) : JobbUtfører` with `companion object : ProvidersJobbSpesifikasjon { konstruer(repositoryProvider, gatewayProvider); type; navn; beskrivelse; retries?; cron? }`.
2. Register it in `prosessering/ProsesseringsJobber.alle()`, or the motor cannot parse stored jobs of that type.
3. Group it: `forBehandling(journalpostId, behandlingId)` or `forSak(journalpostId)` to run in order per journalpost/behandling; nothing for independent or cron jobs.
4. `type` is persisted: never rename it, and keep a stub after removing the feature. Keep payload classes backward compatible.
5. Test the utfører directly with fakes; add a motor-based test only if ordering or retries matter.

## Add an integration (gateway)

1. Interface in `gateway/` (or next to the domain) extending `no.nav.aap.komponenter.gateway.Gateway`.
2. Implementation in `klienter/` with `companion object : Factory<Impl>` (usually a lazily created singleton) reading `requiredConfigForKey("INTEGRASJON_..._URL")`.
3. Register in `klienter/.../DefaultGatewayProvider.kt`. Add env vars and outbound access to `.nais/dev-gcp.yaml` and `.nais/prod-gcp.yaml`.
4. Add a fake (see `postmottak-testing/references/fakes.md`): an HTTP route in `lib-test/.../FakeServers.kt` plus the system property, or an in-code fake. Registering another implementation of the same Gateway interface replaces the earlier one; that is how tests swap in fakes.

## Add an API endpoint

1. In `api/`, as `fun NormalOpenAPIRoute.mittApi(dataSource, repositoryRegistry, ...)`, registered in `App.kt` inside `apiRouting { }`.
2. Use `authorizedGet` / `authorizedPost` from the tilgang plugin. For behandling-scoped routes use `JournalpostPathParam("referanse", journalpostIdFraBehandlingResolver(...))`; mutations need `Operasjon.SAKSBEHANDLE`, operations endpoints `DRIFTE` / `DRIFT_LES`. Plain `get`/`post` (with `@Suppress("UnauthorizedGet")`) are only for local/test routes.
3. Mutating a behandling: take the lock (`TaSkriveLåsRepository.lås`), run `BehandlingTilstandValidator`, mutate, then `verifiserSkrivelås`. Never run steps in the request; enqueue `ProsesserBehandlingJobbUtfører`.
4. Read-only endpoints: `dataSource.transaction(readOnly = true)`.
