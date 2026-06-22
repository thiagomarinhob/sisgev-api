# Especificação Técnica — Aplicativo Mobile
# Sistema de Gestão de Estradas Vicinais (SGEV)

> Derivado do documento `road-vicinais-dev-spec.md`.  
> Este arquivo cobre exclusivamente o app mobile em React Native + TypeScript.

---

## 1. Stack Técnica

```
Framework:    React Native + TypeScript (Expo ou bare workflow)
Navegação:    React Navigation (Stack + Tab)
Banco local:  expo-sqlite ou WatermelonDB
Storage:      expo-secure-store (token) + expo-file-system (fotos)
GPS:          expo-location
Câmera:       expo-camera ou react-native-vision-camera
HTTP Client:  Axios
```

---

## 2. Público-alvo do App

O app é exclusivo para o **Agente de Campo** (`AGENTE_CAMPO`).

Outros papéis não utilizam o app mobile. Toda revisão, classificação e gestão ocorre no portal web.

---

## 3. Estrutura de Pastas

```
mobile/src/
├── app/
│   ├── navigation.tsx        -- Stack e Tab Navigator
│   └── providers.tsx         -- AuthProvider, SyncProvider, DBProvider
├── shared/
│   ├── components/           -- Button, Card, TextInput, GPSIndicator, Badge
│   ├── hooks/                -- useLocation, useSyncQueue, useNetworkStatus
│   ├── services/             -- apiClient, syncService
│   ├── utils/                -- formatDate, gpsAccuracyLabel, uuid
│   └── types/                -- tipos locais e tipos espelhados da API
├── features/
│   ├── auth/                 -- login, armazenamento de token offline
│   ├── inspections/          -- criar e listar vistorias locais
│   ├── camera/               -- captura de foto com GPS integrado
│   ├── location/             -- hook de GPS contínuo e precisão
│   ├── sync/                 -- fila de upload, retry, progresso
│   └── settings/             -- configurações, logout, versão
├── database/
│   ├── schema.ts             -- tabelas SQLite locais
│   ├── migrations.ts         -- migrações do banco local
│   └── repositories/
│       ├── inspectionRepository.ts
│       ├── evidenceRepository.ts
│       └── syncQueueRepository.ts
└── main.tsx
```

---

## 4. Telas

| Tela | Tipo | Descrição |
|---|---|---|
| Login | Stack (pública) | Email + senha; funciona offline se token já salvo |
| Home do Agente | Tab principal | Vistorias recentes, contador de pendências, botão nova vistoria |
| Nova Vistoria | Stack | Cria vistoria local com `client_uuid` |
| Câmera / Coleta de Foto | Stack | Captura foto, exibe GPS em tempo real |
| Lista de Fotos da Vistoria | Stack | Fotos coletadas, status de cada uma |
| Pendências de Sincronização | Tab | Lista de itens não enviados, botão sincronizar |
| Detalhe da Coleta | Stack | Foto + metadados + status de upload |
| Configurações | Tab | URL da API, logout, versão do app |

---

## 5. Fluxo Principal de Coleta

```
1. Agente faz login (requer internet na primeira vez)
   → Token salvo localmente com expo-secure-store

2. Agente inicia nova vistoria
   → Registro criado no banco local com status LOCAL_ONLY
   → client_uuid gerado no device (UUID v4)

3. Agente captura fotos:
   a. App abre câmera
   b. GPS capturado no momento da foto
   c. Foto salva no storage local (expo-file-system)
   d. Metadados salvos no banco local com status LOCAL_ONLY

4. Agente finaliza vistoria
   → Status da vistoria muda para PENDING_SYNC

5. Quando internet disponível (automático ou manual):
   a. Envia metadados da vistoria   → POST /api/v1/inspections
   b. Para cada foto pendente:
      → POST /api/v1/evidences/upload (multipart)
      → Salva remote_id localmente
      → Status → SYNCED
   c. Falha → status ERROR, registro mantido para reenvio
```

---

## 6. Banco de Dados Local (SQLite)

### 6.1. Tabelas

```sql
-- Vistorias criadas offline
CREATE TABLE local_inspections (
    id                INTEGER  PRIMARY KEY AUTOINCREMENT,
    client_uuid       TEXT     NOT NULL UNIQUE,
    remote_id         TEXT     NULL,
    municipality_id   TEXT     NOT NULL,
    field_agent_id    TEXT     NOT NULL,
    sync_status       TEXT     NOT NULL DEFAULT 'LOCAL_ONLY',
    started_at        TEXT     NOT NULL,   -- ISO 8601
    finished_at       TEXT     NULL,
    notes             TEXT     NULL,
    synced_at         TEXT     NULL,
    created_at        TEXT     NOT NULL DEFAULT (datetime('now'))
);

-- Fotos coletadas offline
CREATE TABLE local_evidences (
    id                     INTEGER  PRIMARY KEY AUTOINCREMENT,
    client_uuid            TEXT     NOT NULL UNIQUE,
    remote_id              TEXT     NULL,
    inspection_client_uuid TEXT     NOT NULL,
    local_file_uri         TEXT     NOT NULL,
    latitude               REAL     NOT NULL,
    longitude              REAL     NOT NULL,
    gps_accuracy_meters    REAL     NULL,
    taken_at               TEXT     NOT NULL,   -- ISO 8601 com timezone local
    field_note             TEXT     NULL,
    sync_status            TEXT     NOT NULL DEFAULT 'LOCAL_ONLY',
    upload_attempts        INTEGER  NOT NULL DEFAULT 0,
    last_error             TEXT     NULL,
    created_at             TEXT     NOT NULL DEFAULT (datetime('now'))
);

-- Fila de sincronização
CREATE TABLE sync_queue (
    id              INTEGER  PRIMARY KEY AUTOINCREMENT,
    entity_type     TEXT     NOT NULL,   -- 'inspection' | 'evidence'
    client_uuid     TEXT     NOT NULL,
    status          TEXT     NOT NULL DEFAULT 'QUEUED',
    attempts        INTEGER  NOT NULL DEFAULT 0,
    last_attempt_at TEXT     NULL,
    last_error      TEXT     NULL,
    created_at      TEXT     NOT NULL DEFAULT (datetime('now'))
);

-- Logs locais de erro
CREATE TABLE local_logs (
    id          INTEGER  PRIMARY KEY AUTOINCREMENT,
    level       TEXT     NOT NULL,   -- 'INFO' | 'WARN' | 'ERROR'
    context     TEXT     NOT NULL,
    message     TEXT     NOT NULL,
    created_at  TEXT     NOT NULL DEFAULT (datetime('now'))
);
```

### 6.2. Status de sincronização local

| Status | Significado |
|---|---|
| `LOCAL_ONLY` | Criado só no celular, nunca tentou enviar |
| `QUEUED` | Na fila aguardando envio |
| `UPLOADING` | Envio em andamento |
| `UPLOADED` | Arquivo enviado, confirmação pendente |
| `SYNCED` | Confirmado pelo servidor, `remote_id` salvo |
| `ERROR` | Falha no envio, pode retentar |

---

## 7. Tipos TypeScript

```typescript
type LocalSyncStatus =
  | 'LOCAL_ONLY' | 'QUEUED' | 'UPLOADING' | 'UPLOADED' | 'SYNCED' | 'ERROR';

interface LocalInspection {
  id: number;
  clientUuid: string;
  remoteId: string | null;
  municipalityId: string;
  fieldAgentId: string;
  syncStatus: LocalSyncStatus;
  startedAt: string;
  finishedAt: string | null;
  notes: string | null;
}

interface LocalEvidence {
  id: number;
  clientUuid: string;
  remoteId: string | null;
  inspectionClientUuid: string;
  localFileUri: string;
  latitude: number;
  longitude: number;
  gpsAccuracyMeters: number | null;
  takenAt: string;
  fieldNote: string | null;
  syncStatus: LocalSyncStatus;
  uploadAttempts: number;
  lastError: string | null;
}

interface SyncQueueItem {
  id: number;
  entityType: 'inspection' | 'evidence';
  clientUuid: string;
  status: LocalSyncStatus;
  attempts: number;
}
```

---

## 8. Dados Capturados por Foto

Cada foto deve capturar e persistir localmente:

```json
{
  "clientUuid": "a1b2c3d4-e5f6-...",
  "inspectionClientUuid": "b2c3d4e5-f6a7-...",
  "localFileUri": "file:///data/user/0/com.sgev/cache/photo_001.jpg",
  "latitude": -5.123456,
  "longitude": -39.123456,
  "gpsAccuracyMeters": 12.4,
  "takenAt": "2026-06-21T10:35:00-03:00",
  "fieldNote": "Trecho com lama e buracos",
  "syncStatus": "LOCAL_ONLY"
}
```

---

## 9. Indicador de Precisão GPS

Exibir durante a captura na tela da câmera:

| Precisão | Label | Cor |
|---|---|---|
| até 10 m | Excelente | Verde |
| 10 a 30 m | Aceitável | Amarelo |
| 30 a 50 m | Atenção | Laranja |
| acima de 50 m | Sinal fraco — aguardar | Vermelho |

No MVP, **não bloquear** a captura por precisão ruim. Exibir alerta e deixar o agente decidir.

---

## 10. Sincronização

### 10.1. Regras obrigatórias

1. Todo registro local deve ter `client_uuid` (UUID v4 gerado no device).
2. Upload feito em fila (`sync_queue`), processada em batches configuráveis (padrão: 5).
3. Falha de rede **não** apaga o registro local.
4. Sucesso deve salvar `remote_id` localmente **antes** de marcar como `SYNCED`.
5. Antes de remover foto do storage local, confirmar que `remote_id` foi salvo.
6. App deve suportar fechamento e reabertura sem perder a fila.
7. Agente pode forçar reenvio de itens com status `ERROR`.
8. App deve exibir progresso (`x de y itens enviados`).

### 10.2. Idempotência

O backend ignora reenvios com o mesmo `(field_agent_id, client_uuid)` e retorna o registro existente. O app não precisa controlar duplicatas — basta persistir o `client_uuid` e salvar o `remote_id` retornado.

### 10.3. Ordem de envio

```
1. POST /api/v1/inspections        → envia metadados da vistoria
2. Para cada evidência da vistoria:
   POST /api/v1/evidences/upload   → multipart (arquivo + metadados)
   → salvar remote_id
   → marcar SYNCED
3. Após todas as evidências → marcar vistoria como SYNCED
```

### 10.4. Formato do upload (multipart)

```
POST /api/v1/evidences/upload
Content-Type: multipart/form-data

file:                   <binary>
clientUuid:             uuid
inspectionClientUuid:   uuid
latitude:               -5.123456
longitude:              -39.123456
gpsAccuracyMeters:      12.4
takenAt:                2026-06-21T10:35:00-03:00
fieldNote:              "Trecho com lama"
```

### 10.5. Tratamento de erros de upload

| Código HTTP | Ação no app |
|---|---|
| 200 / 201 | Salvar `remote_id`, marcar SYNCED |
| 409 Conflict | `client_uuid` já existe → salvar `remote_id` do existente, marcar SYNCED |
| 413 Payload Too Large | Comprimir imagem e retentar |
| 422 Unprocessable | Marcar ERROR, exibir motivo ao agente |
| 5xx / timeout | Marcar ERROR, manter na fila para reenvio |

---

## 11. Dados Disponíveis Offline

Após o primeiro login, o app deve funcionar sem internet para:

- Autenticação (token salvo localmente).
- Dados do agente e `municipality_id`.
- Criação de vistorias locais.
- Captura de fotos com GPS.
- Visualização de fotos e vistorias salvas localmente.
- Fila de sincronização persistida.
- Logs locais de erro.

O que **não** está disponível offline:

- Listagem de trechos e estradas (dados do servidor).
- Status de evidências revisadas.
- Histórico de avaliações.

---

## 12. Autenticação Mobile

- Login requer internet.
- `accessToken` armazenado com `expo-secure-store` (keychain/keystore).
- `refreshToken` armazenado com `expo-secure-store`.
- Ao abrir o app offline com token válido: permitir coleta normalmente.
- Ao abrir o app offline com token expirado: exibir aviso e bloquear coleta até reconectar e renovar.
- Logout remove tokens e limpa dados sensíveis do `expo-secure-store`.

---

## 13. Client HTTP (Mobile)

```typescript
// shared/services/apiClient.ts

const apiClient = axios.create({
  baseURL: API_BASE_URL,  // lido de variáveis de ambiente
  timeout: 30000,
});

apiClient.interceptors.request.use(async (config) => {
  const token = await SecureStore.getItemAsync('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      await refreshAccessToken();
      return apiClient(error.config);
    }
    return Promise.reject(error);
  }
);
```

---

## 14. Variáveis de Ambiente

```env
API_BASE_URL=http://10.0.2.2:8080/api/v1    # Android Emulator
# API_BASE_URL=http://localhost:8080/api/v1  # iOS Simulator
SYNC_BATCH_SIZE=5
MAX_UPLOAD_RETRIES=3
GPS_ACCURACY_WARN_THRESHOLD=50
```

---

## 15. Permissões do Sistema Operacional

| Permissão | Momento de solicitar | Obrigatória |
|---|---|---|
| Câmera | Primeira vez que abre a tela de câmera | Sim |
| Localização (precisa) | Primeira vistoria | Sim |
| Localização em background | Não necessário no MVP | Não |
| Storage / Galeria | Primeiro upload (Android < 13) | Sim |

---

## 16. Testes

Priorizar:

- Coleta de foto offline sem internet.
- Banco local: inserção e leitura de vistoria e evidências.
- Fila de sync: persistência após fechamento e reabertura do app.
- Upload com retry após falha de rede.
- Idempotência: reenvio com mesmo `client_uuid` não duplica no servidor.
- GPS: transições corretas entre níveis de precisão.
- Status de sincronização: transições `LOCAL_ONLY → QUEUED → UPLOADING → SYNCED`.
- Tratamento correto de resposta 409 (já enviado).

---

## 17. Ordem de Implementação

1. Configurar React Native + React Navigation + providers.
2. Tela de login com armazenamento seguro de token.
3. Schema SQLite local com repositórios.
4. Home do agente com lista de vistorias locais.
5. Fluxo de nova vistoria (criar local + gerar `client_uuid`).
6. Tela de câmera com captura de foto e GPS em tempo real.
7. Lista de fotos da vistoria com status local.
8. Tela de pendências de sincronização.
9. Serviço de upload com fila, retry e idempotência.
10. Indicador de progresso de sincronização.
11. Tela de configurações e logout.

---

## 18. Definition of Done (Mobile)

- [ ] Coleta funciona completamente offline após primeiro login.
- [ ] Banco local persiste dados entre fechamentos do app.
- [ ] Fila de sync sobrevive a crash e reinicialização do app.
- [ ] Upload é idempotente (reenvio não gera duplicata).
- [ ] `remote_id` salvo antes de marcar item como SYNCED.
- [ ] Foto local não removida antes de confirmação do servidor.
- [ ] Precisão GPS exibida com indicador visual correto.
- [ ] Permissões de câmera e localização solicitadas no momento correto.
- [ ] Tipos TypeScript sem `any` nas interfaces de domínio.
- [ ] Testado em dispositivo físico (não apenas emulador).
