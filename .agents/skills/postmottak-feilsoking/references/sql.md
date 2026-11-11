# Read-only SQL for troubleshooting

Every block below was executed against the migrated schema with data from full flyt tests, including a failed job, a blocked job, a stuck behandling and a behandling på vent. Column names are the real ones (`repository/src/main/resources/flyway/`), and only Postgres 16 features are used (prod runs 16). Use the APIs in `../SKILL.md` for changes, never SQL.

## Before you start

- Connect with your personal user: `nais cloudsql psql --team aap --environment <dev-gcp|prod-gcp> --reason "<ticket>" postmottak-backend` (one-time setup: doc.nais.io → CloudSQL → Personal database access). Prod access is logged.
- Personal users may have **write** privileges here (V1.1 grants ALL to `cloudsqliamuser`). Start every session with the block below.
- Placeholders use psql variables, so blocks run unchanged after `\set journalpost_id 453912345`, `\set behandling_id 123`, `\set fnr 12345678901` (used as `:'fnr'`) and `\set referanse <uuid>` (used as `:'referanse'`). In other clients, replace them with literals.
- Timestamps are stored as Oslo local time without time zone. Set the session time zone before comparing with `now()`.
- `jobb.sak_id` **is the journalpostId** for every journalpost-related job. FERDIG jobs older than 30 days are moved to `jobb_arkiv`/`jobb_historikk_arkiv` nightly (motor cron `jobber.arkiverFerdigstilte`).
- Avoid selecting fødselsnummer, names and free-text columns unless you need them, and never paste them into tickets or chat. `begrunnelse` and `kommentar` are free text.

```sql
set default_transaction_read_only = on;
set time zone 'Europe/Oslo';
```

## One journalpost

### Overview: intake, routing and behandlinger

One row per behandling. If every column is null, postmottak has not accepted the journalpost: the Kafka handler filtered the event, or the `vurderdokument.innkommende` job has not succeeded (see Jobs). A journalpost can have several behandlinger of the same type (for example a new Journalføring when it returns to tema AAP).

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

From a behandlingReferanse (logs, frontend URL):

```sql
select id as behandling_id, journalpost_id, type, status, opprettet_tid
from behandling
where referanse = :'referanse';
```

### Steg history, compact

One line per behandling, oldest first. Each entry is `STEG:phases (time of first row)`. Consecutive rows for the same steg are merged, and each row is one letter: S=START, O=OPPDATER_FAKTAGRUNNLAG, U=UTFØRER, A=AVKLARINGSPUNKT, X=AVSLUTTER, T=TILBAKEFØRT.

- `SOUAX`: a normal pass. Every pass logs A, also when nothing stops.
- Last entry ends with `SOUA`: waiting at this steg for an open avklaringsbehov. `SOUASOUAX`: stopped, resumed later and completed.
- `T`: rewound. One T row is logged for every steg on the way back, including the target, so the informasjonskrav rewind in Journalføring looks like `AVKLAR_SAK:SOUAT > AVKLAR_TEMA:TSOUAX`.
- Last entry ends with `SO`, repeated once per attempt (`SOSOSO`): the step's own code threw. Repeated `S`: refreshing faktagrunnlag threw. Rows from failed attempts survive because of savepoints (`postmottak-flyt-motor/references/motor-jobs-transactions.md`). The error itself is in the Jobs query.

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

### Steg history, every row (one behandling)

```sql
select id, steg, status, aktiv, opprettet_tid
from steg_historikk
where behandling_id = :behandling_id
order by opprettet_tid, id;
```

### Avklaringsbehov: current state

Status is the latest endring. Open means `OPPRETTET` (or `SENDT_TILBAKE_FRA_BESLUTTER`, unused here). A solved DIGITALISER_DOKUMENT may show `AVBRUTT`, see `../SKILL.md`.

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

### Avklaringsbehov: full history (one behandling)

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

### Jobs

All jobs for the journalpost, oldest first, with the number of failed attempts and the latest error. `feil` is the first line of the stored stack trace and `rot_aarsak` the last `Caused by:` line (the same as `feil` when there is none). Expect many `flyt.hendelse` jobs: one per stop, rewind and sett-på-vent. `jobb_historikk` statuses: `KLAR` (created or re-run), `PLUKKET` (attempt started), `FERDIG`, `FEILET` (attempt failed), `AVBRUTT`.

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

Archived jobs (only `FERDIG`, older than 30 days). `jobb_arkiv` has no index on `sak_id`, so this scans the table; run it only when the query above lacks old jobs.

```sql
select id, type, status, behandling_id, opprettet_tid, neste_kjoring
from jobb_arkiv
where sak_id = :journalpost_id
order by id;
```

### Fordeling: rule results and manual decision

How the rules combine is described in `postmottak-dokumentflyt/references/fordeling-og-arena.md`. `system_navn` is the rule outcome; for journalposts with a Fordeling behandling, the decision that was acted on is `avklar_fordeling_vurdering.fagsystem` (next query, `fordeling` rows).

```sql
select rr.system_navn, rr.opprettet_tid, re.regel_navn, re.resultat
from innkommende_journalpost ij
join regelsett_resultat rr on rr.innkommende_journalpost = ij.id
join regel_evaluering re on re.regel_resultat_id = rr.id
where ij.journalpost_id = :journalpost_id
order by re.regel_navn;
```

### Active vurderinger

The saved decisions of all behandlinger for the journalpost (`aktiv` grunnlag rows). A DokumentHåndtering behandling starts with copies of the Journalføring's tema and sak vurderinger (same `tid`). `vurdert_av` is a NAV ident or `Kelvin` (system). `strukturert_dokument` (the digitised form) is left out on purpose: it contains personal data.

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

### Kelvin saker seen by AVKLAR_SAK (one behandling)

The latest snapshot of the person's saker in behandlingsflyt, which drives automatic sak selection and the "avslag på tidligere behandling" checks. Use the Journalføring behandling's id (DokumentHåndtering gets a copy). `SaksnummerInfoKrav` stores a new snapshot only when the result differs from the previous one, so `innhentet` is the time of the last change. No rows: no saker were found, or the behandling never refreshed this faktagrunnlag. `periode` is a `daterange` printed with an exclusive end: `[2025-01-01,2026-01-01)` ends 2025-12-31.

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

### Stored journalpost and documents

The latest journalpost snapshot postmottak saved (one row per document variant). Joark may have changed since; SAF is the source of truth.

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

## One person

Journalposter for a person. `innkommende_journalpost.bruker_id` is the Joark bruker (fødselsnummer or aktør-ID), `person_ident` holds the PDL identer of persons postmottak has looked up. Search with the aktør-ID too if the fødselsnummer gives nothing.

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

## Across all journalposter

### På vent

Open vent-behov (`9001` MANUELT_SATT_PÅ_VENT, `1342` VENT_PA_GOSYS). Add `and b.journalpost_id = :journalpost_id` for one journalpost. `batch.gjenopptaBehandlinger` (daily 07:00) resumes behandlinger with status `UTREDES` whose open behov has `frist <= today`; `skulle_vaert_gjenopptatt` rows are overdue.

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

### Stuck behandlinger

Not `AVSLUTTET`, no open avklaringsbehov and no `KLAR`/`FEILET` job: nothing will move them. `siste_jobb_status` tells why: `AVBRUTT` means someone cancelled the job, `FERDIG` means the flyt returned without finishing (a bug). Fix by enqueuing processing (`POST /api/drift/behandling/{referanse}/prosesser`).

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

### Failed jobs

Per type, then one row per job with its latest error.

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

### Jobs blocked by a failed job

The motor runs jobs with the same `(sak_id, behandling_id, type)` in order, so a `FEILET` job blocks newer `KLAR` jobs of its group. This is why `POST .../prosesser` seems to do nothing while a `flyt.prosesserBehandling` job for the behandling is `FEILET`: re-run or cancel the failed job instead.

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

### Queue and intake health

Overdue `KLAR` jobs per type. A growing number means the motor is not picking jobs (pods down, database problems) or the jobs are blocked (previous query). Cron jobs are always `KLAR` with a future `neste_kjoring` and do not show up here.

```sql
select type, count(*) as antall, min(neste_kjoring) as eldste_neste_kjoring
from jobb
where status = 'KLAR' and neste_kjoring < now() - interval '10 minutes'
group by type
order by antall desc;
```

Open behandlinger per active steg, to see where work is piling up.

```sql
select b.type, sh.steg, sh.status, count(*) as antall, min(sh.opprettet_tid) as eldste
from behandling b
join steg_historikk sh on sh.behandling_id = b.id and sh.aktiv
where b.status <> 'AVSLUTTET'
group by b.type, sh.steg, sh.status
order by b.type, antall desc;
```

Journalposter accepted per hour, last 24 hours. A gap during working hours suggests the Kafka stream has stopped (check `/actuator/live` restarts and stream errors in the logs).

```sql
select date_trunc('hour', opprettet_tid) as time, status, count(*) as antall
from innkommende_journalpost
where opprettet_tid > now() - interval '24 hours'
group by 1, 2
order by 1 desc, 2;
```
