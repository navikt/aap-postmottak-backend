drop index IDX_JOBB_SAK_ID_BEHANDLING_ID_NESTE_KJORING;

create index IDX_JOBB_STATUS_SAK_ID_BEHANDLING_ID_NESTE_KJORING on JOBB (status, sak_id, behandling_id, neste_kjoring) where
    status IN ('FEILET', 'KLAR')
        AND (sak_id is not null OR (sak_id is null and behandling_id is not null));
