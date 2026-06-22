# Especificação Técnica — Backend e Banco de Dados
# Sistema de Gestão de Estradas Vicinais (SGEV)

> Derivado do documento `road-vicinais-dev-spec.md`.  
> Este arquivo cobre exclusivamente o backend Spring Boot e o banco PostgreSQL/PostGIS.

---

## 1. Stack Técnica

```
Backend:        Java 21 + Spring Boot
Banco:          PostgreSQL + PostGIS
Storage:        MinIO (local) / AWS S3 / Cloudflare R2 (produção)
Autenticação:   JWT + Refresh Token
Infra local:    Docker Compose
```

---

## 2. Organização do Projeto Backend

### 2.1. Estrutura de pacotes

```
backend/src/main/java/br/com/sgev
├── SgevApplication.java
├── config
├── security
├── shared
│   ├── exception
│   ├── pagination
│   ├── response
│   ├── validation
│   └── geo
├── auth
├── users
├── municipalities
├── roads
├── roadsegments
├── inspections
├── evidences
├── assessments
├── occurrences
├── maintenance
├── dashboard
├── reports
├── storage
└── audit
```

### 2.2. Padrão de camadas por módulo

Exemplo para `roadsegments`:

```
roadsegments/
├── controller
│   └── RoadSegmentController.java
├── dto
│   ├── RoadSegmentCreateRequest.java
│   ├── RoadSegmentUpdateRequest.java
│   ├── RoadSegmentResponse.java
│   └── RoadSegmentMapResponse.java
├── entity
│   └── RoadSegment.java
├── enums
│   └── RoadCondition.java
├── repository
│   └── RoadSegmentRepository.java
├── service
│   ├── RoadSegmentService.java
│   └── RoadSegmentGeoService.java
└── mapper
    └── RoadSegmentMapper.java
```

### 2.3. Regras de código

- Controller não deve conter regra de negócio.
- Service concentra regra de negócio.
- Repository apenas persistência.
- DTOs não devem expor entidade diretamente.
- Usar validação com Bean Validation (`@Valid`, `@NotNull`, etc.).
- Usar transações (`@Transactional`) em operações de escrita.
- Usar exceptions de domínio com handlers globais (`@ControllerAdvice`).
- Todo endpoint deve validar município/permissão antes de processar.

---

## 3. Banco de Dados

### 3.1. Configuração

- PostgreSQL com extensão **PostGIS** obrigatória.
- SRID: **4326** (WGS84) para todas as geometrias.
- UUID como chave primária em todas as tabelas.
- Timestamps em UTC, sem timezone implícito.

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

### 3.2. Entidades principais

```
users
municipalities
roads
road_segments
inspections
inspection_evidences
road_assessments
occurrences
maintenance_events
audit_logs
refresh_tokens
```

---

## 4. DDL Completo

### 4.1. municipalities

```sql
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
```

---

### 4.2. users

```sql
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
```

- `municipality_id` é NULL apenas para `SUPER_ADMIN`.
- `role` deve ser um dos valores do enum de papéis (ver seção 6).

---

### 4.3. refresh_tokens

```sql
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID            NOT NULL REFERENCES users(id),
    token           TEXT            NOT NULL UNIQUE,
    expires_at      TIMESTAMP       NOT NULL,
    revoked_at      TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);
```

---

### 4.4. roads

```sql
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
```

---

### 4.5. road_segments

```sql
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
```

> `current_condition` é mantido por performance. A fonte histórica é `road_assessments`.

---

### 4.6. inspections

```sql
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
```

---

### 4.7. inspection_evidences

```sql
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
```

---

### 4.8. road_assessments

```sql
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
```

> Esta tabela é append-only. Nunca atualizar registros existentes. Toda reclassificação gera nova linha.

---

### 4.9. occurrences

```sql
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
```

---

### 4.10. maintenance_events

```sql
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
```

---

### 4.11. audit_logs

```sql
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
```

---

## 5. Enums

### 5.1. Papéis de usuário (`role`)

```
SUPER_ADMIN
ADMIN_OPERACIONAL
GESTOR_PREFEITURA
AGENTE_CAMPO
VISUALIZADOR
```

### 5.2. Condição do trecho (`condition` / `current_condition`)

| Valor       | Label         | Severity Score |
|-------------|---------------|---------------:|
| GOOD        | Bom           | 0 – 20         |
| REGULAR     | Regular       | 21 – 40        |
| BAD         | Ruim          | 41 – 70        |
| CRITICAL    | Crítico       | 71 – 90        |
| IMPASSABLE  | Intransitável | 91 – 100       |
| UNKNOWN     | Não avaliado  | —              |

### 5.3. Status da evidência

```
PENDING_UPLOAD
UPLOADED
PENDING_REVIEW
APPROVED
REJECTED
DUPLICATED
INVALID_LOCATION
```

### 5.4. Status da vistoria

```
DRAFT
IN_PROGRESS
PENDING_SYNC
SYNCED
SYNC_ERROR
CLOSED
CANCELLED
```

### 5.5. Status da ocorrência

```
OPEN
IN_ANALYSIS
SCHEDULED
IN_PROGRESS
RESOLVED
CANCELLED
```

### 5.6. Status da intervenção

```
PLANNED
IN_PROGRESS
FINISHED
CANCELLED
```

### 5.7. Tipo de problema (`problem_type`)

```
POTHOLES          -- buracos
MUD               -- lama
FLOODING          -- alagamento
EROSION           -- erosão
BRIDGE_DAMAGE     -- ponte danificada
VEGETATION        -- vegetação invadindo
BLOCKAGE          -- bloqueio
DUST              -- poeira excessiva
DRAINAGE_PROBLEM  -- problema de drenagem
RUTTING           -- trilhas/deformação por roda
OTHER             -- outros
```

---

## 6. Regras de Negócio — Backend

### RN-001 — Multi-tenancy por município

Todo dado operacional deve pertencer a um `municipality_id`.

Em todo Service, validar antes de qualquer operação:

```java
if (!user.isSuperAdmin() && !entity.getMunicipalityId().equals(user.getMunicipalityId())) {
    throw new AccessDeniedException("Acesso negado ao município");
}
```

Nunca confiar em `municipalityId` recebido no payload; usar sempre o do usuário autenticado.

---

### RN-002 — Trecho é a unidade de cálculo

Quilômetros calculados a partir de `road_segments.length_meters`. Fotos são evidências, não unidades de medida.

---

### RN-003 — Histórico imutável de avaliações

`road_assessments` é append-only. Nunca fazer UPDATE nessa tabela. Toda reclassificação insere nova linha.

---

### RN-004 — Status atual do trecho

`road_segments.current_condition` é derivado da avaliação mais recente e mantido por performance.  
Ao inserir em `road_assessments`, atualizar `road_segments.current_condition` e `last_assessment_at` na mesma transação.

---

### RN-005 — Evidência não altera condição automaticamente

Fluxo obrigatório no MVP:

1. Evidência recebida → status `PENDING_REVIEW`.
2. Admin aprova → status `APPROVED`.
3. Admin associa ao trecho (`confirmed_road_segment_id`).
4. Admin cria avaliação referenciando a evidência.
5. Service de avaliação atualiza `current_condition` do trecho.

---

### RN-006 — Evidência rejeitada não afeta dashboard

Evidências com status `REJECTED`, `DUPLICATED` ou `INVALID_LOCATION` devem ser excluídas de:
- cálculos de km por condição;
- indicadores do dashboard;
- relatórios oficiais.

Permanecem disponíveis apenas em telas de auditoria.

---

### RN-007 — GPS obrigatório para evidência oficial

Se `latitude` ou `longitude` estiverem ausentes, salvar evidência com status `INVALID_LOCATION`.  
Se `gps_accuracy_meters > 50`, alertar no response mas não bloquear no MVP.

---

### RN-009 — Upload assíncrono

Foto pode ser enviada depois da coleta. O sistema deve suportar:
- `taken_at` (momento da captura) diferente de `uploaded_at` (momento do envio).
- Múltiplos uploads do mesmo arquivo (idempotência via `client_uuid`).

---

### RN-010 — Timestamps distintos

Armazenar separadamente:
- `taken_at`: momento da captura pelo agente.
- `uploaded_at`: momento do recebimento pelo backend.
- `reviewed_at`: momento da revisão pelo admin.
- `assessed_at`: momento da avaliação oficial do trecho.

---

### RN-015 — Sugestão de trecho por proximidade

Ao receber evidência, o backend deve sugerir o trecho mais próximo dentro de raio configurável (padrão: 100 m).  
Salvar em `suggested_road_segment_id`. Confirmação é sempre manual pelo admin.

---

### RN-017 — Autoria imutável da evidência

`field_agent_id` não pode ser alterado após sincronização, exceto por `SUPER_ADMIN` com log de auditoria.

---

### RN-018 — Auditoria obrigatória

Ações que devem gerar registro em `audit_logs`:

- login / logout;
- criação, edição e exclusão lógica de estrada;
- criação, edição e exclusão lógica de trecho;
- aprovação e rejeição de evidência;
- criação de avaliação;
- alteração de `current_condition`;
- criação, edição e conclusão de intervenção;
- exportação de relatório;
- alteração de permissão/papel de usuário.

---

### RN-019 — Exclusão lógica

Entidades principais não são excluídas fisicamente. Usar:

```sql
deleted_at = NOW()
deleted_by = <user_id>
active     = FALSE
```

Filtrar sempre `deleted_at IS NULL` ou `active = TRUE` em consultas padrão.

---

### RN-025 — Sincronização idempotente

O backend deve verificar `(field_agent_id, client_uuid)` antes de inserir evidência ou vistoria.  
Se já existir, retornar o registro existente com HTTP 200 (não criar duplicata).

---

### RN-027 — Comprimento calculado pela geometria

Ao criar ou atualizar geometria de trecho, calcular `length_meters` automaticamente:

```sql
ST_Length(geometry::geography)
```

Permitir ajuste manual apenas com papel `ADMIN_OPERACIONAL` ou superior e campo `notes` preenchido.

---

### RN-028 — Publicação separada

Trechos e avaliações com `published = FALSE` não aparecem no endpoint do portal da prefeitura.

---

## 7. Consultas Geográficas

### 7.1. Calcular comprimento do trecho ao salvar

```sql
UPDATE road_segments
SET length_meters = ST_Length(geometry::geography)
WHERE id = :id;
```

No JPA, executar via `@PostPersist` / `@PostUpdate` ou dentro do Service antes de salvar.

---

### 7.2. Sugerir trecho mais próximo de uma evidência

```sql
SELECT id, name,
       ST_Distance(
           geometry::geography,
           ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
       ) AS distance_meters
FROM road_segments
WHERE municipality_id = :municipalityId
  AND active = TRUE
  AND deleted_at IS NULL
  AND ST_DWithin(
      geometry::geography,
      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
      :radiusMeters
  )
ORDER BY distance_meters
LIMIT 1;
```

Raio padrão: 100 metros (configurável).

---

### 7.3. Condição histórica por data

```sql
SELECT DISTINCT ON (road_segment_id)
    road_segment_id,
    condition,
    assessed_at
FROM road_assessments
WHERE municipality_id = :municipalityId
  AND assessed_at <= :selectedDate
ORDER BY road_segment_id, assessed_at DESC;
```

Trechos sem avaliação até a data devem ser retornados como `UNKNOWN`.

---

### 7.4. Buscar todos os trechos para o mapa (condição atual)

```sql
SELECT
    rs.id,
    rs.name,
    rs.current_condition,
    rs.length_meters,
    ST_AsGeoJSON(rs.geometry) AS geojson,
    r.name AS road_name
FROM road_segments rs
JOIN roads r ON r.id = rs.road_id
WHERE rs.municipality_id = :municipalityId
  AND rs.active = TRUE
  AND rs.published = TRUE
  AND rs.deleted_at IS NULL;
```

---

## 8. Cálculos do Dashboard

### 8.1. Total mapeado

```
total_mapped_km = SUM(length_meters) / 1000
-- filtro: active = TRUE AND published = TRUE AND deleted_at IS NULL
```

### 8.2. Km por condição (atual)

```sql
SELECT current_condition, SUM(length_meters) / 1000.0 AS km
FROM road_segments
WHERE municipality_id = :municipalityId
  AND active = TRUE
  AND published = TRUE
  AND deleted_at IS NULL
GROUP BY current_condition;
```

### 8.3. Km por condição (histórica — data selecionada)

1. Buscar a última avaliação de cada trecho até a data (query 7.3).
2. JOIN com `road_segments` para obter `length_meters`.
3. Assumir `UNKNOWN` para trechos sem avaliação no período.
4. Somar por condição.

### 8.4. Km recuperados no período

```sql
SELECT SUM(repaired_length_meters) / 1000.0 AS repaired_km
FROM maintenance_events
WHERE municipality_id = :municipalityId
  AND status = 'FINISHED'
  AND finished_date BETWEEN :startDate AND :endDate;
```

### 8.5. Percentual por condição

```
percentual = km_condicao / total_mapped_km * 100
-- Se total_mapped_km = 0, retornar 0 para todos
```

---

## 9. APIs REST

### 9.1. Convenções gerais

- Prefixo: `/api/v1`
- Autenticação: `Authorization: Bearer <accessToken>`
- Paginação: `?page=0&size=20&sort=createdAt,desc`
- Respostas em JSON (UTF-8)

**Resposta paginada:**

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

**Resposta de erro:**

```json
{
  "timestamp": "2026-06-21T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Dados inválidos",
  "details": [
    { "field": "name", "message": "Nome é obrigatório" }
  ]
}
```

---

### 9.2. Endpoints por módulo

#### Auth

```
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
```

#### Municípios

```
GET    /api/v1/municipalities
POST   /api/v1/municipalities
GET    /api/v1/municipalities/{id}
PUT    /api/v1/municipalities/{id}
DELETE /api/v1/municipalities/{id}
```

#### Usuários

```
GET    /api/v1/users
POST   /api/v1/users
GET    /api/v1/users/{id}
PUT    /api/v1/users/{id}
PATCH  /api/v1/users/{id}/activate
PATCH  /api/v1/users/{id}/deactivate
DELETE /api/v1/users/{id}
```

#### Estradas

```
GET    /api/v1/roads
POST   /api/v1/roads
GET    /api/v1/roads/{id}
PUT    /api/v1/roads/{id}
DELETE /api/v1/roads/{id}
POST   /api/v1/roads/import-geojson
```

#### Trechos

```
GET    /api/v1/road-segments
POST   /api/v1/road-segments
GET    /api/v1/road-segments/{id}
PUT    /api/v1/road-segments/{id}
DELETE /api/v1/road-segments/{id}
GET    /api/v1/road-segments/{id}/history
GET    /api/v1/road-segments/{id}/evidences
GET    /api/v1/road-segments/{id}/occurrences
GET    /api/v1/road-segments/{id}/maintenance-events
```

#### Vistorias

```
GET    /api/v1/inspections
POST   /api/v1/inspections
GET    /api/v1/inspections/{id}
PATCH  /api/v1/inspections/{id}/finish
POST   /api/v1/inspections/sync
```

#### Evidências

```
GET    /api/v1/evidences
POST   /api/v1/evidences/upload
GET    /api/v1/evidences/{id}
PATCH  /api/v1/evidences/{id}/approve
PATCH  /api/v1/evidences/{id}/reject
PATCH  /api/v1/evidences/{id}/mark-duplicated
PATCH  /api/v1/evidences/{id}/associate-segment
```

#### Avaliações

```
GET    /api/v1/assessments
POST   /api/v1/assessments
GET    /api/v1/assessments/{id}
GET    /api/v1/road-segments/{segmentId}/assessments
```

#### Ocorrências

```
GET    /api/v1/occurrences
POST   /api/v1/occurrences
GET    /api/v1/occurrences/{id}
PUT    /api/v1/occurrences/{id}
PATCH  /api/v1/occurrences/{id}/status
```

#### Manutenção

```
GET    /api/v1/maintenance-events
POST   /api/v1/maintenance-events
GET    /api/v1/maintenance-events/{id}
PUT    /api/v1/maintenance-events/{id}
PATCH  /api/v1/maintenance-events/{id}/start
PATCH  /api/v1/maintenance-events/{id}/finish
PATCH  /api/v1/maintenance-events/{id}/cancel
```

#### Dashboard

```
GET /api/v1/dashboard/summary
GET /api/v1/dashboard/km-by-condition
GET /api/v1/dashboard/map-segments
GET /api/v1/dashboard/maintenance-summary
GET /api/v1/dashboard/occurrences-summary
```

#### Relatórios

```
GET  /api/v1/reports/summary.csv
GET  /api/v1/reports/summary.pdf
POST /api/v1/reports/custom
```

---

## 10. Exemplos de Payload

### 10.1. Login

Request:
```json
{
  "email": "admin@municipio.gov.br",
  "password": "senha"
}
```

Response:
```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<jwt>",
  "user": {
    "id": "uuid",
    "name": "Admin",
    "email": "admin@municipio.gov.br",
    "role": "ADMIN_OPERACIONAL",
    "municipalityId": "uuid"
  }
}
```

---

### 10.2. Criar trecho

```json
{
  "roadId": "uuid",
  "name": "Trecho 01 - Entrada até Escola Municipal",
  "segmentOrder": 1,
  "geometry": {
    "type": "LineString",
    "coordinates": [
      [-39.123456, -5.123456],
      [-39.124000, -5.124000]
    ]
  },
  "published": true
}
```

> GeoJSON usa `[longitude, latitude]`.

---

### 10.3. Criar avaliação

```json
{
  "roadSegmentId": "uuid",
  "evidenceId": "uuid",
  "condition": "BAD",
  "severityScore": 65,
  "notes": "Buracos e lama, trânsito lento para veículos pequenos.",
  "assessedAt": "2026-06-21T14:30:00-03:00"
}
```

---

### 10.4. Dashboard summary response

```json
{
  "municipalityId": "uuid",
  "period": {
    "startDate": "2026-06-01",
    "endDate": "2026-06-30"
  },
  "totalMappedKm": 42.75,
  "kmByCondition": {
    "GOOD": 18.40,
    "REGULAR": 9.20,
    "BAD": 10.10,
    "CRITICAL": 3.70,
    "IMPASSABLE": 1.35,
    "UNKNOWN": 0.00
  },
  "repairedKm": 8.50,
  "openOccurrences": 12,
  "resolvedOccurrences": 5
}
```

---

## 11. Segurança

### 11.1. JWT — claims mínimos

```json
{
  "sub": "user-id",
  "email": "user@email.com",
  "role": "ADMIN_OPERACIONAL",
  "municipalityId": "uuid",
  "iat": 1750000000,
  "exp": 1750001800
}
```

- `accessToken`: expiração padrão 30 minutos.
- `refreshToken`: expiração padrão 7 dias.

### 11.2. Isolamento por município

```
Se usuário não for SUPER_ADMIN:
    entity.municipality_id DEVE ser igual ao municipalityId do JWT
```

Validar no Service, não apenas no Controller.

### 11.3. Upload de fotos

- MVP: upload direto para o backend, que repassa ao storage.
- Backend gera miniatura após receber o arquivo (síncrono no MVP, assíncrono no futuro).
- Salvar no banco: `file_url`, `thumbnail_url`, `storage_key`, `file_hash`, `file_size_bytes`, `mime_type`.
- Não salvar binário no banco de dados.

---

## 12. Validações

### Usuário
- `name`: obrigatório, 2–150 chars.
- `email`: obrigatório, formato válido, único na tabela.
- `password`: obrigatório na criação, mínimo 8 chars.
- `role`: obrigatório, valor válido do enum.
- `municipalityId`: obrigatório para todos exceto `SUPER_ADMIN`.

### Estrada
- `municipalityId`: obrigatório.
- `name`: obrigatório, 2–180 chars.
- Nome único por município (validar no Service).

### Trecho
- `roadId`: obrigatório.
- `name`: obrigatório.
- `geometry`: obrigatória, tipo `LineString`.
- `length_meters` calculado pela geometria; deve ser > 0.
- `current_condition` inicial: `UNKNOWN`.

### Evidência
- Arquivo de foto: obrigatório.
- `latitude` e `longitude`: obrigatórios.
- `takenAt`: obrigatório.
- `fieldAgentId`: obrigatório.
- `inspectionId`: obrigatório.
- `clientUuid`: obrigatório.

### Avaliação
- `roadSegmentId`: obrigatório.
- `condition`: obrigatório, valor válido do enum.
- `severityScore`: obrigatório, 0–100.
- `assessedBy`: obrigatório (extraído do JWT).
- `assessedAt`: obrigatório.

### Intervenção
- `roadSegmentId`: obrigatório.
- `type`: obrigatório.
- `status`: obrigatório.
- `finishedDate`: obrigatório quando `status = FINISHED`.
- `repairedLengthMeters`: não pode ser negativo.

---

## 13. Testes

### 13.1. Unitários — prioridade

- Cálculo de km por condição atual.
- Cálculo de km por condição histórica (filtro por data).
- Atualização de `current_condition` ao inserir avaliação.
- Validação de multi-tenancy (acesso negado a município diferente).
- Idempotência da sincronização (mesmo `client_uuid`).
- Sugestão de trecho mais próximo.
- Cálculo de km recuperados por período.
- Conclusão de intervenção com `finished_date` obrigatório.

### 13.2. Integração — prioridade

- Login e geração de JWT.
- Refresh token.
- Criação de estrada e trecho com cálculo de comprimento.
- Upload de evidência e criação de miniatura.
- Aprovação de evidência e associação ao trecho.
- Criação de avaliação e atualização de `current_condition`.
- Dashboard summary com km corretos.
- Filtro histórico do mapa por data.
- Exportação de relatório CSV.

---

## 14. Variáveis de Ambiente

```env
SPRING_PROFILES_ACTIVE=dev

# Banco
DATABASE_URL=jdbc:postgresql://localhost:5432/sgev
DATABASE_USERNAME=sgev
DATABASE_PASSWORD=sgev

# JWT
JWT_SECRET=change-me-in-production
JWT_ACCESS_EXPIRATION_MINUTES=30
JWT_REFRESH_EXPIRATION_DAYS=7

# Storage
STORAGE_PROVIDER=minio
STORAGE_BUCKET=sgev-evidences
STORAGE_ENDPOINT=http://localhost:9000
STORAGE_ACCESS_KEY=minioadmin
STORAGE_SECRET_KEY=minioadmin

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:5173

# Geo
GEO_NEAR_SEGMENT_RADIUS_METERS=100
GEO_GPS_ACCURACY_WARN_THRESHOLD_METERS=50
```

---

## 15. Docker Compose — Serviços Locais

```yaml
services:

  postgres:
    image: postgis/postgis:16-3.4
    environment:
      POSTGRES_DB: sgev
      POSTGRES_USER: sgev
      POSTGRES_PASSWORD: sgev
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - miniodata:/data

  backend:
    build: ./backend
    depends_on:
      - postgres
      - minio
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/sgev
      DATABASE_USERNAME: sgev
      DATABASE_PASSWORD: sgev
      STORAGE_ENDPOINT: http://minio:9000
    ports:
      - "8080:8080"

volumes:
  pgdata:
  miniodata:
```

---

## 16. Seed de Desenvolvimento

Criar script `scripts/seed-dev.sql` com:

- 1 município de teste (`Município Teste / CE`).
- 1 usuário `SUPER_ADMIN`.
- 1 usuário `ADMIN_OPERACIONAL`.
- 1 usuário `GESTOR_PREFEITURA`.
- 1 usuário `AGENTE_CAMPO`.
- 3 estradas com geometria.
- 10 trechos com geometrias e comprimentos variados.
- Avaliações distribuídas em todas as condições.
- Algumas evidências fictícias com coordenadas válidas.
- Algumas ocorrências abertas e resolvidas.
- 1 intervenção com status `FINISHED`.

---

## 17. Ordem de Implementação (Backend)

1. Docker Compose com Postgres/PostGIS e MinIO funcionando.
2. Spring Boot base com configuração de DataSource e Flyway/Liquibase.
3. Autenticação JWT (login, refresh, logout, `/me`).
4. CRUD de municípios.
5. CRUD de usuários com papéis.
6. CRUD de estradas.
7. CRUD de trechos com cálculo automático de comprimento (PostGIS).
8. Endpoint de upload de evidências + geração de miniatura.
9. Sugestão automática de trecho próximo por GPS.
10. Endpoints de revisão de evidências (approve, reject, associate).
11. Criação de avaliações + atualização de `current_condition`.
12. Endpoints do dashboard (km por condição, map-segments).
13. Filtro histórico por data no dashboard.
14. CRUD de ocorrências.
15. CRUD de intervenções + conclusão com km recuperados.
16. Exportação de relatórios (CSV primeiro, PDF depois).
17. Auditoria (logar ações críticas em `audit_logs`).

---

## 18. Definition of Done (Backend)

Uma funcionalidade está pronta quando:

- [ ] Regra de negócio implementada no Service.
- [ ] Validações com Bean Validation nos DTOs.
- [ ] Permissão e isolamento de município verificados.
- [ ] Endpoint documentado (mínimo: path, método, request, response).
- [ ] Testes unitários dos casos críticos criados.
- [ ] Tratamento de erro com mensagem clara e código HTTP correto.
- [ ] Multi-tenancy validado (teste com usuário de outro município deve retornar 403).
- [ ] Logs adequados adicionados (INFO em operações normais, ERROR em falhas).
- [ ] Auditoria gerada para ações críticas.
