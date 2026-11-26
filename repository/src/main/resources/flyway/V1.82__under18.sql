ALTER TABLE saksnummer_avklaring ADD COLUMN under18 BOOLEAN NOT NULL DEFAULT FALSE
    CHECK (NOT under18 OR generell_sak);

CREATE TABLE under18_samling (
    behandling_id BIGINT PRIMARY KEY REFERENCES behandling ON DELETE CASCADE,
    person_id BIGINT NOT NULL REFERENCES person,
    tar_imot BOOLEAN NOT NULL
);
CREATE UNIQUE INDEX under18_en_aapen_samling ON under18_samling (person_id) WHERE tar_imot;

CREATE TABLE under18_soknad (
    journalpost_id BIGINT PRIMARY KEY,
    behandling_id BIGINT NOT NULL REFERENCES under18_samling ON DELETE CASCADE,
    posisjon INTEGER NOT NULL,
    UNIQUE (behandling_id, posisjon)
);

CREATE TABLE under18_aldersgrunnlag (
    behandling_id BIGINT PRIMARY KEY REFERENCES behandling ON DELETE CASCADE,
    fodselsdato DATE NOT NULL,
    aldersgruppe TEXT NOT NULL
);

CREATE TABLE under18_soknadsavklaring (
    id BIGSERIAL PRIMARY KEY,
    behandling_id BIGINT NOT NULL REFERENCES behandling ON DELETE CASCADE,
    situasjon TEXT NOT NULL,
    vil_fortsette BOOLEAN NOT NULL,
    opprettet_tid TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX under18_soknadsavklaring_behandling ON under18_soknadsavklaring (behandling_id, id);

CREATE TABLE under18_samtykkevurdering (
    id BIGSERIAL PRIMARY KEY,
    behandling_id BIGINT NOT NULL REFERENCES behandling ON DELETE CASCADE,
    begge_foreldre_samtykket BOOLEAN NOT NULL,
    journalpost_ider BIGINT[] NOT NULL,
    opprettet_tid TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX under18_samtykkevurdering_behandling ON under18_samtykkevurdering (behandling_id, id);
