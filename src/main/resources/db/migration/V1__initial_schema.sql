-- =====================================================================
-- INFRA-03 — Schema inicial do SGEV (PostgreSQL + PostGIS)
-- SRID 4326 (WGS84) em todas as geometrias. UUID como PK. Timestamps UTC.
-- Fonte: docs/backend-database-spec.md §4
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- municipalities
-- ---------------------------------------------------------------------
CREATE TABLE municipalities (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(150)    NOT NULL,
    state           CHAR(2)         NOT NULL,
    ibge_code       VARCHAR(20),
    boundary        GEOMETRY(MULTIPOLYGON, 4326),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP       NULL
);

-- ---------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    municipality_id UUID            NULL REFERENCES municipalities(id),
    name            VARCHAR(150)    NOT NULL,
    email           VARCHAR(180)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(50)     NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP       NULL
);

-- ---------------------------------------------------------------------
-- refresh_tokens
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID            NOT NULL REFERENCES users(id),
    token           TEXT            NOT NULL UNIQUE,
    expires_at      TIMESTAMP       NOT NULL,
    revoked_at      TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- roads
-- ---------------------------------------------------------------------
CREATE TABLE roads (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    municipality_id      UUID            NOT NULL REFERENCES municipalities(id),
    name                 VARCHAR(180)    NOT NULL,
    description          TEXT,
    geometry             GEOMETRY(MULTILINESTRING, 4326),
    total_length_meters  NUMERIC(12,2),
    active               BOOLEAN         NOT NULL DEFAULT TRUE,
    published            BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted_at           TIMESTAMP       NULL
);

CREATE INDEX idx_roads_municipality ON roads(municipality_id);
CREATE INDEX idx_roads_geometry ON roads USING GIST(geometry);

-- ---------------------------------------------------------------------
-- road_segments
-- ---------------------------------------------------------------------
CREATE TABLE road_segments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    municipality_id     UUID            NOT NULL REFERENCES municipalities(id),
    road_id             UUID            NOT NULL REFERENCES roads(id),
    name                VARCHAR(180)    NOT NULL,
    segment_order       INTEGER,
    geometry            GEOMETRY(LINESTRING, 4326) NOT NULL,
    length_meters       NUMERIC(12,2)   NOT NULL,
    current_condition   VARCHAR(30)     NOT NULL DEFAULT 'UNKNOWN',
    last_assessment_at  TIMESTAMP       NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    published           BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMP       NULL
);

CREATE INDEX idx_road_segments_municipality ON road_segments(municipality_id);
CREATE INDEX idx_road_segments_road        ON road_segments(road_id);
CREATE INDEX idx_road_segments_geometry    ON road_segments USING GIST(geometry);
CREATE INDEX idx_road_segments_condition   ON road_segments(current_condition);

-- ---------------------------------------------------------------------
-- inspections
-- ---------------------------------------------------------------------
CREATE TABLE inspections (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    municipality_id UUID            NOT NULL REFERENCES municipalities(id),
    field_agent_id  UUID            NOT NULL REFERENCES users(id),
    client_uuid     UUID            NOT NULL,
    status          VARCHAR(30)     NOT NULL,
    started_at      TIMESTAMP       NOT NULL,
    finished_at     TIMESTAMP       NULL,
    synced_at       TIMESTAMP       NULL,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE (field_agent_id, client_uuid)
);

CREATE INDEX idx_inspections_municipality ON inspections(municipality_id);
CREATE INDEX idx_inspections_agent        ON inspections(field_agent_id);
CREATE INDEX idx_inspections_status       ON inspections(status);

-- ---------------------------------------------------------------------
-- inspection_evidences
-- ---------------------------------------------------------------------
CREATE TABLE inspection_evidences (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    municipality_id             UUID            NOT NULL REFERENCES municipalities(id),
    inspection_id               UUID            NOT NULL REFERENCES inspections(id),
    field_agent_id              UUID            NOT NULL REFERENCES users(id),
    suggested_road_segment_id   UUID            NULL REFERENCES road_segments(id),
    confirmed_road_segment_id   UUID            NULL REFERENCES road_segments(id),
    client_uuid                 UUID            NOT NULL,
    file_url                    TEXT            NOT NULL,
    thumbnail_url               TEXT            NULL,
    storage_key                 TEXT            NOT NULL,
    mime_type                   VARCHAR(80),
    file_size_bytes             BIGINT,
    file_hash                   VARCHAR(128),
    latitude                    NUMERIC(10,7)   NOT NULL,
    longitude                   NUMERIC(10,7)   NOT NULL,
    location                    GEOMETRY(POINT, 4326) NOT NULL,
    gps_accuracy_meters         NUMERIC(8,2),
    taken_at                    TIMESTAMP       NOT NULL,
    uploaded_at                 TIMESTAMP       NOT NULL,
    reviewed_at                 TIMESTAMP       NULL,
    reviewed_by                 UUID            NULL REFERENCES users(id),
    status                      VARCHAR(30)     NOT NULL DEFAULT 'PENDING_REVIEW',
    field_note                  TEXT,
    admin_note                  TEXT,
    created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE (field_agent_id, client_uuid)
);

CREATE INDEX idx_evidences_municipality ON inspection_evidences(municipality_id);
CREATE INDEX idx_evidences_inspection   ON inspection_evidences(inspection_id);
CREATE INDEX idx_evidences_status       ON inspection_evidences(status);
CREATE INDEX idx_evidences_location     ON inspection_evidences USING GIST(location);
CREATE INDEX idx_evidences_taken_at     ON inspection_evidences(taken_at);

-- ---------------------------------------------------------------------
-- road_assessments  (append-only)
-- ---------------------------------------------------------------------
CREATE TABLE road_assessments (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    municipality_id  UUID            NOT NULL REFERENCES municipalities(id),
    road_segment_id  UUID            NOT NULL REFERENCES road_segments(id),
    evidence_id      UUID            NULL REFERENCES inspection_evidences(id),
    condition        VARCHAR(30)     NOT NULL,
    severity_score   INTEGER         NOT NULL CHECK (severity_score BETWEEN 0 AND 100),
    source           VARCHAR(30)     NOT NULL DEFAULT 'MANUAL',
    notes            TEXT,
    assessed_by      UUID            NOT NULL REFERENCES users(id),
    assessed_at      TIMESTAMP       NOT NULL,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_assessments_segment      ON road_assessments(road_segment_id);
CREATE INDEX idx_assessments_municipality ON road_assessments(municipality_id);
CREATE INDEX idx_assessments_assessed_at  ON road_assessments(assessed_at);
CREATE INDEX idx_assessments_condition    ON road_assessments(condition);

-- ---------------------------------------------------------------------
-- occurrences
-- ---------------------------------------------------------------------
CREATE TABLE occurrences (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    municipality_id  UUID            NOT NULL REFERENCES municipalities(id),
    road_segment_id  UUID            NOT NULL REFERENCES road_segments(id),
    evidence_id      UUID            NULL REFERENCES inspection_evidences(id),
    problem_type     VARCHAR(50)     NOT NULL,
    status           VARCHAR(30)     NOT NULL DEFAULT 'OPEN',
    severity_score   INTEGER         NOT NULL CHECK (severity_score BETWEEN 0 AND 100),
    description      TEXT,
    opened_by        UUID            NOT NULL REFERENCES users(id),
    opened_at        TIMESTAMP       NOT NULL,
    resolved_at      TIMESTAMP       NULL,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_occurrences_municipality ON occurrences(municipality_id);
CREATE INDEX idx_occurrences_segment      ON occurrences(road_segment_id);
CREATE INDEX idx_occurrences_status       ON occurrences(status);

-- ---------------------------------------------------------------------
-- maintenance_events
-- ---------------------------------------------------------------------
CREATE TABLE maintenance_events (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    municipality_id         UUID            NOT NULL REFERENCES municipalities(id),
    road_segment_id         UUID            NOT NULL REFERENCES road_segments(id),
    occurrence_id           UUID            NULL REFERENCES occurrences(id),
    type                    VARCHAR(80)     NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'PLANNED',
    planned_start_date      DATE            NULL,
    actual_start_date       DATE            NULL,
    finished_date           DATE            NULL,
    repaired_length_meters  NUMERIC(12,2)   CHECK (repaired_length_meters >= 0),
    notes                   TEXT,
    created_by              UUID            NOT NULL REFERENCES users(id),
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_maintenance_municipality ON maintenance_events(municipality_id);
CREATE INDEX idx_maintenance_segment      ON maintenance_events(road_segment_id);
CREATE INDEX idx_maintenance_status       ON maintenance_events(status);

-- ---------------------------------------------------------------------
-- audit_logs
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    municipality_id UUID            NULL REFERENCES municipalities(id),
    user_id         UUID            NULL REFERENCES users(id),
    action          VARCHAR(100)    NOT NULL,
    entity_name     VARCHAR(100)    NOT NULL,
    entity_id       UUID            NULL,
    old_values      JSONB           NULL,
    new_values      JSONB           NULL,
    ip_address      VARCHAR(80),
    user_agent      TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_municipality ON audit_logs(municipality_id);
CREATE INDEX idx_audit_user         ON audit_logs(user_id);
CREATE INDEX idx_audit_entity       ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_created_at   ON audit_logs(created_at);
