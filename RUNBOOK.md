# RUNBOOK

Oppskrifter på hvordan aktuelle operasjonelle oppgaver kan gjennomføres. 


## Database

### Hente ut fødselsnummer for søknader fra personer med Arenahistorikk sendt til Kelvin
Se eksempelet nedenfor. Det gjelder personer som er representert i AAP-Arena, 
og har sendt inn en søknad i en bestemt periode. 
Slik går det an å finne ut hvilke søknader som er fordelt til Arena i perioden 
og møter bestemte kriterier (bestemte regel-resultat). 

```SQL 
select distinct (bruker_id)
from public.regel_evaluering as eval
         join regelsett_resultat result on result.id = eval.regel_resultat_id
         join innkommende_journalpost as inkommende on result.innkommende_journalpost = inkommende.id
where eval.regel_navn = 'ArenaSakRegel' -- sjekk om personen eksisterer i Arena
  and eval.resultat = true
  and result.system_navn = 'KELVIN' -- ble fordelt til Kelvin
  and brevkode = 'NAV 11-13.05' -- søknad
  and result.opprettet_tid >= TIMESTAMP '2026-09-21 08:33:00'
  and result.opprettet_tid <= TIMESTAMP '2026-09-21 13:09:00';
```
Spørring for Arena-databasen for å se hvilke `person_id` dette gjelder: 
```SQL
SELECT person_id FROM person WHERE person.fodselsnr IN (
    'verdi fra forrige spørring', 'verdi fra forrige spørring');
```

