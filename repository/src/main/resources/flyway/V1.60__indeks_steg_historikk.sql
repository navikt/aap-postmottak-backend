DROP INDEX uidx_steg_historikk;

create index idx_steg_historikk_behandling_tid
    on steg_historikk (behandling_id, opprettet_tid);