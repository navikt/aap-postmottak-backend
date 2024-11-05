ALTER TABLE behandling ADD COLUMN REFERANSE uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE behandling DROP CONSTRAINT behandling_journalpost_id_key;