ALTER TABLE saksnummer_avklaring ADD COLUMN under18 BOOLEAN NOT NULL DEFAULT FALSE
    CHECK (NOT under18 OR generell_sak);

CREATE TABLE under18_samling (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES person,
    journalpost_ider BIGINT[] NOT NULL
);

CREATE TABLE under18_samlingsgrunnlag (
    id BIGSERIAL PRIMARY KEY,
    behandling_id BIGINT NOT NULL REFERENCES behandling,
    samling_id BIGINT NOT NULL REFERENCES under18_samling,
    opprettet_tid TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX under18_aktiv_samling ON under18_samlingsgrunnlag (behandling_id) WHERE aktiv;
CREATE INDEX under18_samling_historikk ON under18_samlingsgrunnlag (behandling_id, id);

CREATE TABLE under18_soknadstilknytning (
    journalpost_id BIGINT PRIMARY KEY,
    behandling_id BIGINT NOT NULL REFERENCES behandling
);
CREATE INDEX under18_soknadstilknytning_behandling ON under18_soknadstilknytning (behandling_id);

CREATE TABLE under18_aldersvurdering (
    id BIGSERIAL PRIMARY KEY,
    fodselsdato DATE NOT NULL,
    aldersgruppe TEXT NOT NULL
);
CREATE TABLE under18_aldersgrunnlag (
    id BIGSERIAL PRIMARY KEY,
    behandling_id BIGINT NOT NULL REFERENCES behandling,
    aldersvurdering_id BIGINT NOT NULL REFERENCES under18_aldersvurdering,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    opprettet_tid TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX under18_aktivt_aldersgrunnlag ON under18_aldersgrunnlag (behandling_id) WHERE aktiv;

CREATE TABLE under18_soknadsavklaring (
    id BIGSERIAL PRIMARY KEY,
    situasjon TEXT,
    vil_fortsette BOOLEAN,
    fylt_17_aar_og_9_maaneder BOOLEAN
);
CREATE TABLE under18_soknadsavklaring_grunnlag (
    id BIGSERIAL PRIMARY KEY,
    behandling_id BIGINT NOT NULL REFERENCES behandling,
    soknadsavklaring_id BIGINT NOT NULL REFERENCES under18_soknadsavklaring,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    opprettet_tid TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX under18_aktiv_soknadsavklaring ON under18_soknadsavklaring_grunnlag (behandling_id) WHERE aktiv;

CREATE TABLE under18_samtykkevurdering (
    id BIGSERIAL PRIMARY KEY,
    begge_foreldre_samtykket BOOLEAN NOT NULL,
    journalpost_ider BIGINT[] NOT NULL
);
CREATE TABLE under18_samtykkegrunnlag (
    id BIGSERIAL PRIMARY KEY,
    behandling_id BIGINT NOT NULL REFERENCES behandling,
    samtykkevurdering_id BIGINT NOT NULL REFERENCES under18_samtykkevurdering,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    opprettet_tid TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX under18_aktiv_samtykkevurdering ON under18_samtykkegrunnlag (behandling_id) WHERE aktiv;