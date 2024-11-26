alter table dokument drop column ident;
alter table journalpost add column kanal varchar(100) not null default 'UKJENT';