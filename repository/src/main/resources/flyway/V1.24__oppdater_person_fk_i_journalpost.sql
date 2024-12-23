ALTER TABLE journalpost
    DROP COLUMN aktoer_ident;
ALTER TABLE journalpost
    ADD COLUMN person_id BIGINT;
ALTER TABLE journalpost
    ADD CONSTRAINT journalpost_person_id_fk FOREIGN KEY (person_id) REFERENCES person (id);
ALTER TABLE journalpost
    DROP COLUMN person_ident