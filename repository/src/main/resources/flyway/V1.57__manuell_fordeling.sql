create table manuell_fordeling(
    id bigserial primary key,
    ident text not null,
    fagsystem text not null check (fagsystem in ('kelvin', 'arena'))
);

create unique index uidx_manuell_fordeling_ident on manuell_fordeling(ident);
