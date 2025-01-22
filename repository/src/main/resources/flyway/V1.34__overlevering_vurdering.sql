CREATE TABLE overlevering_vurdering
(
    ID               BIGSERIAL PRIMARY KEY,
    SKAL_OVERLEVERES BOOLEAN                                NOT NULL,
    TIDSSTEMPEL      TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE overlevering_grunnlag
(
    ID                        BIGSERIAL                              NOT NULL PRIMARY KEY,
    BEHANDLING_ID             BIGINT                                 NOT NULL REFERENCES BEHANDLING (ID),
    OVERLEVERING_VURDERING_ID BIGINT                                 NOT NULL REFERENCES overlevering_vurdering (ID),
    OPPRETTET_TID             TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    AKTIV                     BOOLEAN      DEFAULT TRUE              NOT NULL
);

CREATE INDEX BARE_EN_AKTIV_OVERLEVERING_PÅ_BEHANDLING ON overlevering_grunnlag (BEHANDLING_ID, AKTIV) WHERE AKTIV;