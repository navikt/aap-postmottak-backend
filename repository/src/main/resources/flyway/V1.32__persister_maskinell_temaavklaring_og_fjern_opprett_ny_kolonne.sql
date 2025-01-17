-- Denne kolonnen brukes til å lagre maskinelt utledet tema basert på skal_til_aap
ALTER TABLE temavurdering
    ADD COLUMN tema VARCHAR(100) NOT NULL DEFAULT 'UKJENT';

UPDATE temavurdering
SET tema = 'AAP'
WHERE skal_til_aap = TRUE;

UPDATE temavurdering
SET tema = 'OPP'
WHERE skal_til_aap = FALSE
  and 'NAV 08-07.08' IN (SELECT brevkode FROM dokument
                                  JOIN journalpost ON dokument.journalpost_id = journalpost.id
                                  JOIN behandling ON journalpost.id = behandling.journalpost_id
                                  JOIN temavurdering_grunnlag ON behandling.id = temavurdering_grunnlag.behandling_id
                         WHERE temavurdering_grunnlag.temavurdering_id = temavurdering.id);

ALTER TABLE saksnummer_avklaring DROP COLUMN opprett_ny;