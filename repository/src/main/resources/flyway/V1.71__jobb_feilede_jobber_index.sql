-- Nytt felt for å støtte plukkJobbV2 som er mer effektiv enn plukkJobb
alter table jobb
    add column kjorbar boolean not null default false;

--    Sørger for unikhet på jobb for en sak/behandling og type med nye plukkV2
CREATE UNIQUE INDEX UX_JOBB_EKSKLUSIV_AKTIV ON JOBB (COALESCE(SAK_ID, -1), COALESCE(BEHANDLING_ID, -1), TYPE)
    WHERE (STATUS = 'FEILET' OR (STATUS = 'KLAR' AND KJORBAR))
        AND (SAK_ID IS NOT NULL OR BEHANDLING_ID IS NOT NULL);

-- plukkJobbV2 hot-path
CREATE INDEX IDX_JOBB_PLUKKBAR ON JOBB (NESTE_KJORING)
    WHERE STATUS = 'KLAR' AND KJORBAR;

-- hentInfoOmSiste - tar lang tid i Paw Patrol uten denne
CREATE INDEX IDX_JOBB_TERMINAL_NESTE_KJORING ON JOBB (NESTE_KJORING)
    WHERE STATUS = 'FERDIG' OR STATUS = 'FEILET';