# Lesende SQL for feilsøking

Hver blokk under er kjørt mot det migrerte skjemaet med data fra fulle flyt-tester, blant annet en feilet jobb, en blokkert jobb, en behandling som står fast og en behandling på vent. Kolonnenavnene er de reelle (`repository/src/main/resources/flyway/`), og bare Postgres 16-funksjoner er brukt (prod kjører 16). Bruk API-ene i `../SKILL.md` til endringer, aldri SQL.

## Før du starter

- Koble til med din personlige bruker: `nais cloudsql psql --team aap --environment <dev-gcp|prod-gcp> --reason "<ticket>" postmottak-backend` (engangsoppsett: doc.nais.io → CloudSQL → Personal database access). Tilgang til prod logges.
- Personlige brukere kan ha **skrivetilgang** her (V1.1 gir ALL til `cloudsqliamuser`). Start hver økt med blokken under.
- Plassholderne bruker psql-variabler, så blokkene kjører uendret etter `\set journalpost_id 453912345`, `\set behandling_id 123`, `\set fnr 12345678901` (brukes som `:'fnr'`) og `\set referanse <uuid>` (brukes som `:'referanse'`). I andre klienter erstatter du dem med litteraler.
- Tidsstempler lagres som lokal Oslo-tid uten tidssone. Sett tidssonen for økta før du sammenligner med `now()`.
- `jobb.sak_id` **er journalpostId** for alle journalpostrelaterte jobber. FERDIG-jobber som er eldre enn 30 dager, flyttes hver natt til `jobb_arkiv`/`jobb_historikk_arkiv` (motor-cron `jobber.arkiverFerdigstilte`).
- Ikke hent ut fødselsnummer, navn og fritekstkolonner med mindre du trenger dem, og lim dem aldri inn i saker eller chat. `begrunnelse` og `kommentar` er fritekst.

```sql
set default_transaction_read_only = on;
set time zone 'Europe/Oslo';
```

## Én journalpost

### Oversikt: inntak, ruting og behandlinger

Én rad per behandling. Hvis alle kolonnene er null, har ikke postmottak godtatt journalposten: Kafka-handleren filtrerte bort eventet, eller jobben `vurderdokument.innkommende` har ikke gått bra (se Jobber). En journalpost kan ha flere behandlinger av samme type (for eksempel en ny Journalføring når den kommer tilbake til tema AAP).

```sql
select ij.status as innkommende_status, ij.aarsak_til_status, ij.brevkode, ij.behandlingstema, ij.enhet,
       ij.opprettet_tid as mottatt, rr.system_navn as regelresultat,
       b.id as behandling_id, b.referanse, b.type, b.status as behandling_status, b.opprettet_tid as behandling_opprettet,
       sh.steg as aktivt_steg, sh.status as steg_status, sh.opprettet_tid as steg_tid
from (select :journalpost_id::bigint as journalpost_id) p
left join innkommende_journalpost ij on ij.journalpost_id = p.journalpost_id
left join regelsett_resultat rr on rr.innkommende_journalpost = ij.id
left join behandling b on b.journalpost_id = p.journalpost_id
left join steg_historikk sh on sh.behandling_id = b.id and sh.aktiv
order by b.id;
```

Fra en behandlingReferanse (logger, frontend-URL):

```sql
select id as behandling_id, journalpost_id, type, status, opprettet_tid
from behandling
where referanse = :'referanse';
```

### Steghistorikk, kompakt

Én linje per behandling, eldste først. Hver oppføring er `STEG:faser (tidspunkt for første rad)`. Etterfølgende rader for samme steg slås sammen, og hver rad er én bokstav: S=START, O=OPPDATER_FAKTAGRUNNLAG, U=UTFØRER, A=AVKLARINGSPUNKT, X=AVSLUTTER, T=TILBAKEFØRT.

- `SOUAX`: en vanlig gjennomkjøring. Hver gjennomkjøring logger A, også når ingenting stopper.
- Siste oppføring slutter på `SOUA`: venter ved dette steget på et åpent avklaringsbehov. `SOUASOUAX`: stoppet, gjenopptatt senere og fullført.
- `T`: rullet tilbake. Én T-rad logges for hvert steg på veien tilbake, også målsteget, så tilbakerullingen for informasjonskrav i Journalføring ser slik ut: `AVKLAR_SAK:SOUAT > AVKLAR_TEMA:TSOUAX`.
- Siste oppføring slutter på `SO`, gjentatt én gang per forsøk (`SOSOSO`): stegets egen kode kastet exception. Gjentatt `S`: oppdatering av faktagrunnlag kastet exception. Rader fra mislykkede forsøk overlever på grunn av savepoints (`postmottak-flyt-motor/references/motor-jobs-transactions.md`). Selve feilen står i spørringen for jobber.

```sql
with h as (
    select sh.behandling_id, sh.steg, sh.status, sh.opprettet_tid, sh.id,
           row_number() over (partition by sh.behandling_id order by sh.opprettet_tid, sh.id)
         - row_number() over (partition by sh.behandling_id, sh.steg order by sh.opprettet_tid, sh.id) as besok
    from steg_historikk sh
    join behandling b on b.id = sh.behandling_id
    where b.journalpost_id = :journalpost_id
), besok as (
    select behandling_id, steg, besok, min(opprettet_tid) as fra, min(id) as forste_id,
           string_agg(case status when 'START' then 'S' when 'OPPDATER_FAKTAGRUNNLAG' then 'O'
                                  when 'UTFØRER' then 'U' when 'AVKLARINGSPUNKT' then 'A'
                                  when 'AVSLUTTER' then 'X' when 'TILBAKEFØRT' then 'T' else '?' end,
                      '' order by opprettet_tid, id) as faser
    from h
    group by behandling_id, steg, besok
)
select b.id as behandling_id, b.type, b.status,
       string_agg(v.steg || ':' || v.faser || ' (' || to_char(v.fra, 'MM-DD HH24:MI') || ')', ' > '
                  order by v.fra, v.forste_id) as steg_historikk
from besok v
join behandling b on b.id = v.behandling_id
group by b.id, b.type, b.status
order by b.id;
```

### Steghistorikk, hver rad (én behandling)

```sql
select id, steg, status, aktiv, opprettet_tid
from steg_historikk
where behandling_id = :behandling_id
order by opprettet_tid, id;
```

### Avklaringsbehov: nåværende tilstand

Status er siste endring. Åpen betyr `OPPRETTET` (eller `SENDT_TILBAKE_FRA_BESLUTTER`, ubrukt her). En løst DIGITALISER_DOKUMENT kan vises som `AVBRUTT`, se `../SKILL.md`.

```sql
select b.id as behandling_id, b.type, a.id as avklaringsbehov_id,
       case a.definisjon
           when '1338' then 'DIGITALISER_DOKUMENT' when '1339' then 'AVKLAR_TEMA'
           when '1340' then 'AVKLAR_SAK'           when '1341' then 'AVKLAR_OVERLEVERING'
           when '1342' then 'VENT_PA_GOSYS'        when '1343' then 'AVKLAR_FORDELING'
           when '9001' then 'MANUELT_SATT_PÅ_VENT' else a.definisjon end as definisjon,
       a.funnet_i_steg, siste.status, siste.frist, siste.venteaarsak, siste.opprettet_av,
       siste.opprettet_tid as endret, siste.antall_endringer
from behandling b
join avklaringsbehov a on a.behandling_id = b.id
cross join lateral (
    select e.status, e.frist, e.venteaarsak, e.opprettet_av, e.opprettet_tid,
           count(*) over () as antall_endringer
    from avklaringsbehov_endring e
    where e.avklaringsbehov_id = a.id
    order by e.opprettet_tid desc, e.id desc
    limit 1
) siste
where b.journalpost_id = :journalpost_id
order by b.id, a.id;
```

### Avklaringsbehov: full historikk (én behandling)

```sql
select a.definisjon, e.status, e.opprettet_tid, e.opprettet_av, e.frist, e.venteaarsak,
       left(e.begrunnelse, 100) as begrunnelse,
       (select string_agg(r.aarsak_til_retur, ', ' order by r.id)
        from avklaringsbehov_endring_aarsak r where r.endring_id = e.id) as aarsak_til_retur
from avklaringsbehov a
join avklaringsbehov_endring e on e.avklaringsbehov_id = a.id
where a.behandling_id = :behandling_id
order by e.opprettet_tid, e.id;
```

### Jobber

Alle jobber for journalposten, eldste først, med antall mislykkede forsøk og siste feil. `feil` er første linje i den lagrede stacktracen, og `rot_aarsak` er siste `Caused by:`-linje (samme som `feil` hvis det ikke finnes noen). Forvent mange `flyt.hendelse`-jobber: én per stopp, tilbakerulling og sett-på-vent. Statuser i `jobb_historikk`: `KLAR` (opprettet eller kjørt på nytt), `PLUKKET` (forsøk startet), `FERDIG`, `FEILET` (forsøk mislyktes), `AVBRUTT`.

```sql
select j.id, j.type, j.status, j.behandling_id, j.opprettet_tid, j.neste_kjoring,
       (select count(*) from jobb_historikk jh where jh.jobb_id = j.id and jh.status = 'FEILET') as antall_feil,
       sf.opprettet_tid as siste_feil_tid,
       split_part(sf.feilmelding, E'\n', 1) as feil,
       split_part(split_part(sf.feilmelding, 'Caused by: ', -1), E'\n', 1) as rot_aarsak
from jobb j
left join lateral (
    select jh.feilmelding, jh.opprettet_tid
    from jobb_historikk jh
    where jh.jobb_id = j.id and jh.status = 'FEILET'
    order by jh.opprettet_tid desc, jh.id desc
    limit 1
) sf on true
where j.sak_id = :journalpost_id
order by j.id;
```

Arkiverte jobber (bare `FERDIG`, eldre enn 30 dager). `jobb_arkiv` har ingen indeks på `sak_id`, så spørringen skanner hele tabellen. Kjør den bare når spørringen over mangler gamle jobber.

```sql
select id, type, status, behandling_id, opprettet_tid, neste_kjoring
from jobb_arkiv
where sak_id = :journalpost_id
order by id;
```

### Fordeling: regelresultater og manuell beslutning

Hvordan reglene kombineres, står i `postmottak-dokumentflyt/references/fordeling-og-arena.md`. `system_navn` er utfallet av reglene. For journalposter med en Fordeling-behandling er beslutningen som ble fulgt `avklar_fordeling_vurdering.fagsystem` (neste spørring, `fordeling`-radene).

```sql
select rr.system_navn, rr.opprettet_tid, re.regel_navn, re.resultat
from innkommende_journalpost ij
join regelsett_resultat rr on rr.innkommende_journalpost = ij.id
join regel_evaluering re on re.regel_resultat_id = rr.id
where ij.journalpost_id = :journalpost_id
order by re.regel_navn;
```

### Aktive vurderinger

De lagrede beslutningene fra alle behandlinger for journalposten (`aktiv` grunnlagsrader). En DokumentHåndtering-behandling starter med kopier av tema- og saksvurderingene fra Journalføring (samme `tid`). `vurdert_av` er en Nav-ident eller `Kelvin` (systemet). `strukturert_dokument` (det digitaliserte skjemaet) er med vilje utelatt fordi det inneholder personopplysninger.

```sql
select b.id as behandling_id, b.type, 'tema' as vurdering, t.tidsstempel as tid,
       format('skal_til_aap=%s tema=%s', t.skal_til_aap, t.tema) as verdi
from behandling b
join temavurdering_grunnlag g on g.behandling_id = b.id and g.aktiv
join temavurdering t on t.id = g.temavurdering_id
where b.journalpost_id = :journalpost_id
union all
select b.id, b.type, 'sak', s.tidsstempel,
       format('saksnummer=%s generell_sak=%s opprettet_ny=%s', s.saksnummer, s.generell_sak, s.opprettet_ny)
from behandling b
join saksvurdering_grunnlag g on g.behandling_id = b.id and g.aktiv
join saksnummer_avklaring s on s.id = g.saksnummer_avklaring_id
where b.journalpost_id = :journalpost_id
union all
select b.id, b.type, 'fordeling', v.opprettet_tid,
       format('fagsystem=%s vurdert_av=%s kommentar=%s', v.fagsystem, v.vurdert_av, left(v.kommentar, 100))
from behandling b
join avklar_fordeling_grunnlag g on g.behandling_id = b.id and g.aktiv
join avklar_fordeling_vurdering v on v.id = g.avklar_fordeling_vurdering_id
where b.journalpost_id = :journalpost_id
union all
select b.id, b.type, 'digitalisering', d.tidsstempel,
       format('kategori=%s soknadsdato=%s digitalisert_manuelt_postmottak=%s',
              d.kategori, d.soknadsdato, d.digitalisert_manuelt_postmottak)
from behandling b
join digitaliseringsvurdering_grunnlag g on g.behandling_id = b.id and g.aktiv
join digitaliseringsavklaring d on d.id = g.digitaliseringsavklaring_id
where b.journalpost_id = :journalpost_id
union all
select b.id, b.type, 'overlevering', o.tidsstempel,
       format('skal_overleveres=%s begrunnelse=%s', o.skal_overleveres, left(o.begrunnelse, 100))
from behandling b
join overlevering_grunnlag g on g.behandling_id = b.id and g.aktiv
join overlevering_vurdering o on o.id = g.overlevering_vurdering_id
where b.journalpost_id = :journalpost_id
order by behandling_id, tid;
```

### Kelvin-saker sett av AVKLAR_SAK (én behandling)

Siste øyeblikksbilde av personens saker i behandlingsflyt. Det styrer automatisk saksvalg og sjekkene for «avslag på tidligere behandling». Bruk id-en til Journalføring-behandlingen (DokumentHåndtering får en kopi). `SaksnummerInfoKrav` lagrer et nytt øyeblikksbilde bare når resultatet er forskjellig fra det forrige, så `innhentet` er tidspunktet for siste endring. Ingen rader: det ble ikke funnet noen saker, eller behandlingen har aldri oppdatert dette faktagrunnlaget. `periode` er en `daterange` som skrives ut med eksklusiv slutt: `[2025-01-01,2026-01-01)` slutter 2025-12-31.

```sql
select s.saksnummer, s.periode, s.avslag, s.resultatkode, s.finnes_aapen_behandling,
       s.har_rett_naa_eller_i_framtiden, i.opprettet as innhentet
from innhentede_saker_for_behandling i
join saker_paa_behandling s on s.innhentede_saker_for_behandling_id = i.id
where i.id = (select id from innhentede_saker_for_behandling
              where behandling_id = :behandling_id
              order by opprettet desc, id desc
              limit 1)
order by s.saksnummer;
```

### Lagret journalpost og dokumenter

Siste journalpost-øyeblikksbilde postmottak har lagret (én rad per dokumentvariant). Joark kan ha endret seg siden. SAF er sannhetskilden.

```sql
select j.status, j.tema, j.behandlingstema, j.kanal, j.saksnummer, j.fagsystem, j.journalforende_enhet,
       j.mottatt_dato, j.mottatt_tid, j.opprettet_tid as lagret,
       d.dokument_info_id, d.brevkode, d.variantformat, d.filtype
from (select * from journalpost
      where journalpost_id = :journalpost_id
      order by opprettet_tid desc, id desc
      limit 1) j
left join dokument d on d.journalpost_id = j.id
order by d.dokument_info_id, d.variantformat;
```

## Én person

Journalposter for en person. `innkommende_journalpost.bruker_id` er Joark-brukeren (fødselsnummer eller aktør-ID), og `person_ident` inneholder PDL-identene til personer postmottak har slått opp. Søk også med aktør-ID hvis fødselsnummeret ikke gir treff.

```sql
with jp as (
    select journalpost_id from innkommende_journalpost where bruker_id = :'fnr'
    union
    select j.journalpost_id
    from person_ident pi
    join journalpost j on j.person_id = pi.person_id
    where pi.ident = :'fnr'
)
select jp.journalpost_id, ij.status as innkommende_status, ij.brevkode, ij.opprettet_tid as mottatt,
       rr.system_navn as regelresultat,
       (select string_agg(b.type || ':' || b.status, ', ' order by b.id)
        from behandling b where b.journalpost_id = jp.journalpost_id) as behandlinger
from jp
left join innkommende_journalpost ij on ij.journalpost_id = jp.journalpost_id
left join regelsett_resultat rr on rr.innkommende_journalpost = ij.id
order by ij.opprettet_tid desc nulls last, jp.journalpost_id;
```

## På tvers av journalposter

### På vent

Åpne vent-behov (`9001` MANUELT_SATT_PÅ_VENT, `1342` VENT_PA_GOSYS). Legg til `and b.journalpost_id = :journalpost_id` for én journalpost. `batch.gjenopptaBehandlinger` (daglig 07:00) gjenopptar behandlinger med status `UTREDES` der det åpne behovet har `frist <= today`. Rader med `skulle_vaert_gjenopptatt` er forfalt.

```sql
select b.id as behandling_id, b.journalpost_id, b.type, b.status as behandling_status,
       a.definisjon, siste.frist, siste.venteaarsak, siste.opprettet_av, siste.opprettet_tid as satt_paa_vent,
       siste.frist < current_date as skulle_vaert_gjenopptatt
from avklaringsbehov a
join behandling b on b.id = a.behandling_id
cross join lateral (
    select e.status, e.frist, e.venteaarsak, e.opprettet_av, e.opprettet_tid
    from avklaringsbehov_endring e
    where e.avklaringsbehov_id = a.id
    order by e.opprettet_tid desc, e.id desc
    limit 1
) siste
where a.definisjon in ('9001', '1342')
  and siste.status = 'OPPRETTET'
order by siste.frist, b.id;
```

### Fastlåste behandlinger

Ikke `AVSLUTTET`, ingen åpne avklaringsbehov og ingen `KLAR`/`FEILET` jobb: ingenting vil flytte dem. `siste_jobb_status` forklarer hvorfor: `AVBRUTT` betyr at noen avbrøt jobben, `FERDIG` betyr at flyten returnerte uten å fullføre (en bug). Løs det ved å legge behandlingen i kø for prosessering (`POST /api/drift/behandling/{referanse}/prosesser`).

```sql
with stuck as materialized (
    select b.id, b.journalpost_id, b.referanse, b.type, b.status, b.opprettet_tid
    from behandling b
    where b.status <> 'AVSLUTTET'
      and not exists (select 1 from jobb j
                      where j.behandling_id = b.id and j.status in ('KLAR', 'FEILET'))
      and not exists (
          select 1
          from avklaringsbehov a
          cross join lateral (
              select e.status from avklaringsbehov_endring e
              where e.avklaringsbehov_id = a.id
              order by e.opprettet_tid desc, e.id desc
              limit 1
          ) siste
          where a.behandling_id = b.id
            and siste.status in ('OPPRETTET', 'SENDT_TILBAKE_FRA_BESLUTTER'))
)
select s.id as behandling_id, s.journalpost_id, s.referanse, s.type, s.status, s.opprettet_tid,
       sh.steg as aktivt_steg, sh.status as steg_status, sh.opprettet_tid as steg_tid,
       sj.type as siste_jobb, sj.status as siste_jobb_status, sj.opprettet_tid as siste_jobb_tid
from stuck s
left join steg_historikk sh on sh.behandling_id = s.id and sh.aktiv
left join lateral (
    select j.type, j.status, j.opprettet_tid
    from jobb j
    where j.behandling_id = s.id
    order by j.id desc
    limit 1
) sj on true
order by s.opprettet_tid;
```

### Feilede jobber

Per type, deretter én rad per jobb med siste feil.

```sql
select type, count(*) as antall, min(opprettet_tid) as eldste, max(opprettet_tid) as nyeste
from jobb
where status = 'FEILET'
group by type
order by antall desc;
```

```sql
select j.id, j.type, j.sak_id as journalpost_id, j.behandling_id, j.opprettet_tid,
       (select count(*) from jobb_historikk jh where jh.jobb_id = j.id and jh.status = 'FEILET') as antall_feil,
       split_part(sf.feilmelding, E'\n', 1) as feil,
       split_part(split_part(sf.feilmelding, 'Caused by: ', -1), E'\n', 1) as rot_aarsak
from jobb j
left join lateral (
    select jh.feilmelding
    from jobb_historikk jh
    where jh.jobb_id = j.id and jh.status = 'FEILET'
    order by jh.opprettet_tid desc, jh.id desc
    limit 1
) sf on true
where j.status = 'FEILET'
order by j.type, j.opprettet_tid;
```

### Jobber blokkert av en feilet jobb

Motoren kjører jobber med samme `(sak_id, behandling_id, type)` i rekkefølge, så en `FEILET` jobb blokkerer nyere `KLAR` jobber i gruppen. Derfor ser det ut som `POST .../prosesser` ikke gjør noe når en `flyt.prosesserBehandling`-jobb for behandlingen er `FEILET`: kjør den feilede jobben på nytt eller avbryt den i stedet.

```sql
select k.id, k.type, k.sak_id as journalpost_id, k.behandling_id, k.opprettet_tid, f.id as blokkert_av_jobb
from jobb k
join jobb f on f.status = 'FEILET'
           and f.type = k.type
           and f.sak_id is not distinct from k.sak_id
           and f.behandling_id is not distinct from k.behandling_id
           and f.neste_kjoring <= k.neste_kjoring
where k.status = 'KLAR'
order by k.opprettet_tid;
```

### Kø og inntak

Forfalte `KLAR` jobber per type. Et økende antall betyr at motoren ikke plukker jobber (pods nede, databaseproblemer) eller at jobbene er blokkert (forrige spørring). Cron-jobber er alltid `KLAR` med en `neste_kjoring` i framtida og vises ikke her.

```sql
select type, count(*) as antall, min(neste_kjoring) as eldste_neste_kjoring
from jobb
where status = 'KLAR' and neste_kjoring < now() - interval '10 minutes'
group by type
order by antall desc;
```

Åpne behandlinger per aktivt steg, for å se hvor arbeid hoper seg opp.

```sql
select b.type, sh.steg, sh.status, count(*) as antall, min(sh.opprettet_tid) as eldste
from behandling b
join steg_historikk sh on sh.behandling_id = b.id and sh.aktiv
where b.status <> 'AVSLUTTET'
group by b.type, sh.steg, sh.status
order by b.type, antall desc;
```

Journalposter godtatt per time, siste 24 timer. Et opphold i arbeidstida tyder på at Kafka-streamen har stoppet (sjekk restarter på `/actuator/live` og stream-feil i loggene).

```sql
select date_trunc('hour', opprettet_tid) as time, status, count(*) as antall
from innkommende_journalpost
where opprettet_tid > now() - interval '24 hours'
group by 1, 2
order by 1 desc, 2;
```
