-- =====================================================================
-- Seed de desenvolvimento — SisGEV
-- Senha de TODOS os usuários: Senha@123
-- Execute: psql -h localhost -U sgev -d sgev -f scripts/seed-dev.sql
-- =====================================================================

BEGIN;

-- =====================================================================
-- Município de Teste
-- =====================================================================
INSERT INTO municipalities (id, name, state, ibge_code, active)
VALUES ('00000001-0000-0000-0000-000000000001', 'Município Teste', 'CE', '2300000', TRUE)
ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- Usuários — senha de todos: Senha@123
-- =====================================================================
INSERT INTO users (id, municipality_id, name, email, password_hash, role, active) VALUES
  ('00000000-0000-0000-0000-000000000001',
   NULL,
   'Super Admin', 'super@sgev.com',
   '$2a$10$tyQyQTjo2VhbkptoI8lb/udApjmaaH6oMn4PGDkOURipXhcctNHB2',
   'SUPER_ADMIN', TRUE),

  ('00000000-0000-0000-0000-000000000002',
   '00000001-0000-0000-0000-000000000001',
   'Admin Operacional', 'admin@municipio.gov.br',
   '$2a$10$8DNlpbIdqGtrDPllFOCbQuzc3NxkclYxmrj0pv5Ubwo2cL.iv78zS',
   'ADMIN_OPERACIONAL', TRUE),

  ('00000000-0000-0000-0000-000000000003',
   '00000001-0000-0000-0000-000000000001',
   'Gestor Prefeitura', 'gestor@municipio.gov.br',
   '$2a$10$XRY6pb89Hua12IhLujBBDu/Avrc4r4AbKuKMyEHY79d13HHbZlvte',
   'GESTOR_PREFEITURA', TRUE),

  ('00000000-0000-0000-0000-000000000004',
   '00000001-0000-0000-0000-000000000001',
   'Agente de Campo', 'agente@municipio.gov.br',
   '$2a$10$awZq/XUQ3vNo0xG4nTliBeRk67QH7Lr9MxMnpE3KZYGp0GPPiYylm',
   'AGENTE_CAMPO', TRUE),

  ('00000000-0000-0000-0000-000000000005',
   '00000001-0000-0000-0000-000000000001',
   'Visualizador', 'visualizador@municipio.gov.br',
   '$2a$10$.q/.xxtmMnKDrOJC30j4L.huu03BNsi0nyu/PAWS.xm2IvN28zIMm',
   'VISUALIZADOR', TRUE)
ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- Estradas (3) — coordenadas na região de Sobral/CE
-- =====================================================================
INSERT INTO roads (id, municipality_id, name, description, geometry, total_length_meters, active, published) VALUES
  ('00000000-0000-0000-0001-000000000001',
   '00000001-0000-0000-0000-000000000001',
   'Estrada Vicinal EV-001', 'Acesso à zona rural norte',
   ST_GeomFromText('MULTILINESTRING((-39.1200 -4.5600, -39.1300 -4.5700, -39.1400 -4.5800, -39.1500 -4.5900))', 4326),
   4660, TRUE, TRUE),

  ('00000000-0000-0000-0001-000000000002',
   '00000001-0000-0000-0000-000000000001',
   'Estrada Vicinal EV-002', 'Ligação entre distritos',
   ST_GeomFromText('MULTILINESTRING((-39.2000 -4.6000, -39.2100 -4.6100, -39.2200 -4.6200))', 4326),
   3110, TRUE, TRUE),

  ('00000000-0000-0000-0001-000000000003',
   '00000001-0000-0000-0000-000000000001',
   'Estrada Vicinal EV-003', 'Acesso à área agrícola sul',
   ST_GeomFromText('MULTILINESTRING((-39.3000 -4.7000, -39.3100 -4.7100, -39.3200 -4.7200))', 4326),
   3110, TRUE, FALSE)
ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- Trechos (10) — condições distribuídas em todas as categorias
-- =====================================================================
INSERT INTO road_segments (id, municipality_id, road_id, name, segment_order, geometry, length_meters, current_condition, active, published) VALUES
  -- EV-001 (4 trechos)
  ('00000000-0000-0000-0002-000000000001', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000001',
   'EV-001 Trecho 01', 1, ST_GeomFromText('LINESTRING(-39.1200 -4.5600, -39.1250 -4.5650)', 4326), 780, 'GOOD', TRUE, TRUE),

  ('00000000-0000-0000-0002-000000000002', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000001',
   'EV-001 Trecho 02', 2, ST_GeomFromText('LINESTRING(-39.1250 -4.5650, -39.1300 -4.5700)', 4326), 780, 'REGULAR', TRUE, TRUE),

  ('00000000-0000-0000-0002-000000000003', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000001',
   'EV-001 Trecho 03', 3, ST_GeomFromText('LINESTRING(-39.1300 -4.5700, -39.1400 -4.5800)', 4326), 1550, 'BAD', TRUE, TRUE),

  ('00000000-0000-0000-0002-000000000004', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000001',
   'EV-001 Trecho 04', 4, ST_GeomFromText('LINESTRING(-39.1400 -4.5800, -39.1500 -4.5900)', 4326), 1550, 'CRITICAL', TRUE, TRUE),

  -- EV-002 (3 trechos)
  ('00000000-0000-0000-0002-000000000005', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000002',
   'EV-002 Trecho 01', 1, ST_GeomFromText('LINESTRING(-39.2000 -4.6000, -39.2100 -4.6100)', 4326), 1550, 'GOOD', TRUE, TRUE),

  ('00000000-0000-0000-0002-000000000006', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000002',
   'EV-002 Trecho 02', 2, ST_GeomFromText('LINESTRING(-39.2100 -4.6100, -39.2150 -4.6150)', 4326), 780, 'IMPASSABLE', TRUE, TRUE),

  ('00000000-0000-0000-0002-000000000007', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000002',
   'EV-002 Trecho 03', 3, ST_GeomFromText('LINESTRING(-39.2150 -4.6150, -39.2200 -4.6200)', 4326), 780, 'REGULAR', TRUE, TRUE),

  -- EV-003 (3 trechos)
  ('00000000-0000-0000-0002-000000000008', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000003',
   'EV-003 Trecho 01', 1, ST_GeomFromText('LINESTRING(-39.3000 -4.7000, -39.3100 -4.7100)', 4326), 1550, 'BAD', TRUE, FALSE),

  ('00000000-0000-0000-0002-000000000009', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000003',
   'EV-003 Trecho 02', 2, ST_GeomFromText('LINESTRING(-39.3100 -4.7100, -39.3150 -4.7150)', 4326), 780, 'CRITICAL', TRUE, FALSE),

  ('00000000-0000-0000-0002-000000000010', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000003',
   'EV-003 Trecho 03', 3, ST_GeomFromText('LINESTRING(-39.3150 -4.7150, -39.3200 -4.7200)', 4326), 780, 'UNKNOWN', TRUE, FALSE)
ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- Avaliações — uma por trecho (exceto trecho 10 que fica UNKNOWN)
-- =====================================================================
INSERT INTO road_assessments (id, municipality_id, road_segment_id, condition, severity_score, source, notes, assessed_by, assessed_at) VALUES
  ('00000000-0000-0000-0003-000000000001', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000001',
   'GOOD',      10, 'MANUAL', 'Boa condição, sem buracos visíveis',              '00000000-0000-0000-0000-000000000002', NOW() - INTERVAL '10 days'),
  ('00000000-0000-0000-0003-000000000002', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000002',
   'REGULAR',   30, 'MANUAL', 'Alguns pontos de erosão superficial',             '00000000-0000-0000-0000-000000000002', NOW() - INTERVAL '9 days'),
  ('00000000-0000-0000-0003-000000000003', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000003',
   'BAD',       55, 'MANUAL', 'Buracos frequentes, lama em dias de chuva',       '00000000-0000-0000-0000-000000000002', NOW() - INTERVAL '8 days'),
  ('00000000-0000-0000-0003-000000000004', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000004',
   'CRITICAL',  80, 'MANUAL', 'Erosão severa na lateral, risco de bloqueio',     '00000000-0000-0000-0000-000000000002', NOW() - INTERVAL '7 days'),
  ('00000000-0000-0000-0003-000000000005', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000005',
   'GOOD',      15, 'MANUAL', 'Boa condição após manutenção recente',            '00000000-0000-0000-0000-000000000002', NOW() - INTERVAL '6 days'),
  ('00000000-0000-0000-0003-000000000006', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000006',
   'IMPASSABLE', 95, 'MANUAL', 'Alagamento total, via interditada',              '00000000-0000-0000-0000-000000000002', NOW() - INTERVAL '5 days'),
  ('00000000-0000-0000-0003-000000000007', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000007',
   'REGULAR',   25, 'MANUAL', 'Desgaste superficial',                            '00000000-0000-0000-0000-000000000002', NOW() - INTERVAL '4 days'),
  ('00000000-0000-0000-0003-000000000008', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000008',
   'BAD',       60, 'MANUAL', 'Trilhas fundas por tráfego pesado',               '00000000-0000-0000-0000-000000000002', NOW() - INTERVAL '3 days'),
  ('00000000-0000-0000-0003-000000000009', '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000009',
   'CRITICAL',  75, 'MANUAL', 'Corte de barreira, risco de deslizamento',        '00000000-0000-0000-0000-000000000002', NOW() - INTERVAL '2 days')
ON CONFLICT (id) DO NOTHING;

UPDATE road_segments SET last_assessment_at = NOW() - INTERVAL '10 days' WHERE id = '00000000-0000-0000-0002-000000000001';
UPDATE road_segments SET last_assessment_at = NOW() - INTERVAL '9 days'  WHERE id = '00000000-0000-0000-0002-000000000002';
UPDATE road_segments SET last_assessment_at = NOW() - INTERVAL '8 days'  WHERE id = '00000000-0000-0000-0002-000000000003';
UPDATE road_segments SET last_assessment_at = NOW() - INTERVAL '7 days'  WHERE id = '00000000-0000-0000-0002-000000000004';
UPDATE road_segments SET last_assessment_at = NOW() - INTERVAL '6 days'  WHERE id = '00000000-0000-0000-0002-000000000005';
UPDATE road_segments SET last_assessment_at = NOW() - INTERVAL '5 days'  WHERE id = '00000000-0000-0000-0002-000000000006';
UPDATE road_segments SET last_assessment_at = NOW() - INTERVAL '4 days'  WHERE id = '00000000-0000-0000-0002-000000000007';
UPDATE road_segments SET last_assessment_at = NOW() - INTERVAL '3 days'  WHERE id = '00000000-0000-0000-0002-000000000008';
UPDATE road_segments SET last_assessment_at = NOW() - INTERVAL '2 days'  WHERE id = '00000000-0000-0000-0002-000000000009';

-- =====================================================================
-- Vistorias (2)
-- =====================================================================
INSERT INTO inspections (id, municipality_id, field_agent_id, client_uuid, status, started_at, finished_at, synced_at, notes) VALUES
  ('00000000-0000-0000-0004-000000000001',
   '00000001-0000-0000-0000-000000000001',
   '00000000-0000-0000-0000-000000000004',
   '11111111-aaaa-bbbb-cccc-000000000001',
   'SYNCED',
   NOW() - INTERVAL '5 days',
   NOW() - INTERVAL '5 days' + INTERVAL '4 hours',
   NOW() - INTERVAL '4 days',
   'Vistoria de rotina EV-001'),

  ('00000000-0000-0000-0004-000000000002',
   '00000001-0000-0000-0000-000000000001',
   '00000000-0000-0000-0000-000000000004',
   '11111111-aaaa-bbbb-cccc-000000000002',
   'CLOSED',
   NOW() - INTERVAL '2 days',
   NOW() - INTERVAL '2 days' + INTERVAL '3 hours',
   NULL,
   'Verificação após chuvas EV-002 e EV-003')
ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- Evidências fictícias (3)
-- =====================================================================
INSERT INTO inspection_evidences (
  id, municipality_id, inspection_id, field_agent_id,
  suggested_road_segment_id, client_uuid,
  file_url, storage_key, mime_type, file_size_bytes, file_hash,
  latitude, longitude, location, gps_accuracy_meters,
  taken_at, uploaded_at, status, field_note
) VALUES
  ('00000000-0000-0000-0005-000000000001',
   '00000001-0000-0000-0000-000000000001',
   '00000000-0000-0000-0004-000000000001',
   '00000000-0000-0000-0000-000000000004',
   '00000000-0000-0000-0002-000000000003',
   '22222222-aaaa-bbbb-cccc-000000000001',
   'http://localhost:9000/sgev-evidences/2026/06/ev001-t03-01.jpg',
   '2026/06/ev001-t03-01.jpg', 'image/jpeg', 1240000, 'abc123def456',
   -4.5750, -39.1350, ST_SetSRID(ST_MakePoint(-39.1350, -4.5750), 4326), 5.0,
   NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', 'APPROVED',
   'Buraco no meio da pista, veículos desviando para o acostamento'),

  ('00000000-0000-0000-0005-000000000002',
   '00000001-0000-0000-0000-000000000001',
   '00000000-0000-0000-0004-000000000001',
   '00000000-0000-0000-0000-000000000004',
   '00000000-0000-0000-0002-000000000004',
   '22222222-aaaa-bbbb-cccc-000000000002',
   'http://localhost:9000/sgev-evidences/2026/06/ev001-t04-01.jpg',
   '2026/06/ev001-t04-01.jpg', 'image/jpeg', 980000, 'def789ghi012',
   -4.5850, -39.1450, ST_SetSRID(ST_MakePoint(-39.1450, -4.5850), 4326), 8.0,
   NOW() - INTERVAL '5 days' + INTERVAL '1 hour', NOW() - INTERVAL '5 days' + INTERVAL '1 hour', 'APPROVED',
   'Erosão lateral severa, risco de desbarrancamento'),

  ('00000000-0000-0000-0005-000000000003',
   '00000001-0000-0000-0000-000000000001',
   '00000000-0000-0000-0004-000000000002',
   '00000000-0000-0000-0000-000000000004',
   '00000000-0000-0000-0002-000000000006',
   '22222222-aaaa-bbbb-cccc-000000000003',
   'http://localhost:9000/sgev-evidences/2026/06/ev002-t02-01.jpg',
   '2026/06/ev002-t02-01.jpg', 'image/jpeg', 1560000, 'ghi345jkl678',
   -4.6150, -39.2150, ST_SetSRID(ST_MakePoint(-39.2150, -4.6150), 4326), 3.0,
   NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 'PENDING_REVIEW',
   'Via completamente alagada, impossível transitar')
ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- Ocorrências (4 — mix de status)
-- =====================================================================
INSERT INTO occurrences (id, municipality_id, road_segment_id, problem_type, status, severity_score, description, opened_by, opened_at, resolved_at) VALUES
  ('00000000-0000-0000-0006-000000000001',
   '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000003',
   'POTHOLES', 'RESOLVED', 55, 'Série de buracos no trecho 03 da EV-001',
   '00000000-0000-0000-0000-000000000004', NOW() - INTERVAL '30 days', NOW() - INTERVAL '10 days'),

  ('00000000-0000-0000-0006-000000000002',
   '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000004',
   'EROSION', 'OPEN', 80, 'Erosão severa na lateral do trecho 04, risco de colapso',
   '00000000-0000-0000-0000-000000000004', NOW() - INTERVAL '7 days', NULL),

  ('00000000-0000-0000-0006-000000000003',
   '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000006',
   'FLOODING', 'OPEN', 95, 'Alagamento total na EV-002 trecho 02 após chuvas',
   '00000000-0000-0000-0000-000000000004', NOW() - INTERVAL '2 days', NULL),

  ('00000000-0000-0000-0006-000000000004',
   '00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000009',
   'EROSION', 'IN_ANALYSIS', 75, 'Corte de barreira no trecho 02 da EV-003',
   '00000000-0000-0000-0000-000000000004', NOW() - INTERVAL '2 days', NULL)
ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- Intervenção (1 FINISHED com km recuperados)
-- =====================================================================
INSERT INTO maintenance_events (
  id, municipality_id, road_segment_id, occurrence_id,
  type, status,
  planned_start_date, actual_start_date, finished_date,
  repaired_length_meters, notes, created_by
) VALUES (
  '00000000-0000-0000-0007-000000000001',
  '00000001-0000-0000-0000-000000000001',
  '00000000-0000-0000-0002-000000000003',
  '00000000-0000-0000-0006-000000000001',
  'Patching', 'FINISHED',
  (NOW() - INTERVAL '25 days')::DATE,
  (NOW() - INTERVAL '20 days')::DATE,
  (NOW() - INTERVAL '10 days')::DATE,
  1550.00,
  'Recuperação de buracos com cascalho compactado. Trecho reaberto ao tráfego.',
  '00000000-0000-0000-0000-000000000002'
) ON CONFLICT (id) DO NOTHING;

COMMIT;
