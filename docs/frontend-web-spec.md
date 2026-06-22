# Especificação Técnica — Frontend Web
# Sistema de Gestão de Estradas Vicinais (SGEV)

> Derivado do documento `road-vicinais-dev-spec.md`.  
> Este arquivo cobre exclusivamente o frontend web em React + TypeScript.

---

## 1. Stack Técnica

```
Framework:    React + TypeScript
Build:        Vite
Mapas:        MapLibre GL JS
HTTP Client:  Axios
Estilização:  Tailwind CSS (sugerido) ou similar
```

---

## 2. Aplicações e Responsabilidades

O projeto web é uma única aplicação React com roteamento por perfil:

| Interface | Público | Finalidade |
|---|---|---|
| Portal Admin | `ADMIN_OPERACIONAL`, `SUPER_ADMIN` | Revisar evidências, classificar trechos, gerenciar dados |
| Portal Prefeitura | `GESTOR_PREFEITURA`, `VISUALIZADOR` | Visualizar dashboard, mapa e relatórios |

---

## 3. Estrutura de Pastas

```
web/src/
├── app/
│   ├── router.tsx          -- rotas protegidas por perfil
│   ├── providers.tsx       -- QueryClient, AuthProvider, MapProvider
│   └── guards.tsx          -- PrivateRoute, RoleGuard
├── shared/
│   ├── components/         -- Button, Modal, Table, Badge, Spinner...
│   ├── hooks/              -- useAuth, useMunicipality, usePagination
│   ├── services/           -- apiClient (axios configurado)
│   ├── types/              -- interfaces TypeScript dos domínios
│   ├── utils/              -- formatKm, formatDate, conditionColor
│   └── constants/          -- CONDITION_COLORS, ROLES, API_PATHS
├── features/
│   ├── auth/
│   ├── dashboard/
│   ├── map/
│   ├── roads/
│   ├── segments/
│   ├── evidences/
│   ├── assessments/
│   ├── occurrences/
│   ├── maintenance/
│   └── reports/
└── main.tsx
```

### 3.1. Estrutura interna por feature

```
features/evidences/
├── components/
│   ├── EvidenceReviewPanel.tsx
│   ├── EvidenceCard.tsx
│   └── EvidenceStatusBadge.tsx
├── hooks/
│   └── useEvidences.ts
├── services/
│   └── evidencesService.ts
├── types/
│   └── evidence.types.ts
└── pages/
    └── EvidencesPage.tsx
```

---

## 4. Roteamento

```
/login

-- Admin
/admin/dashboard
/admin/municipalities
/admin/users
/admin/roads
/admin/segments
/admin/segments/:id
/admin/evidences
/admin/evidences/:id
/admin/assessments
/admin/occurrences
/admin/maintenance
/admin/reports

-- Portal Prefeitura
/prefeitura/dashboard
/prefeitura/mapa
/prefeitura/trechos/:id
/prefeitura/relatorios
```

### 4.1. Proteção de rotas

- `/admin/*`: apenas `SUPER_ADMIN` e `ADMIN_OPERACIONAL`.
- `/prefeitura/*`: apenas `GESTOR_PREFEITURA` e `VISUALIZADOR`.
- `/login`: redirecionar para dashboard se já autenticado.
- Não autenticado: redirecionar para `/login`.
- Após login, redirecionar pela role:
  - `ADMIN_OPERACIONAL` → `/admin/dashboard`
  - `GESTOR_PREFEITURA` → `/prefeitura/dashboard`

---

## 5. Autenticação

### 5.1. Fluxo

1. Usuário envia email + senha em `/login`.
2. API retorna `accessToken`, `refreshToken` e dados do usuário.
3. `accessToken` armazenado em memória (contexto React — nunca em `localStorage`).
4. `refreshToken` armazenado em `localStorage` ou `httpOnly cookie` (alinhar com backend).
5. `apiClient` intercepta respostas 401 e tenta refresh automático.
6. Se refresh falhar, redirecionar para `/login`.

### 5.2. AuthContext

```typescript
interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  municipalityId: string | null;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}
```

### 5.3. Regras obrigatórias

- Nunca filtrar dados por `municipalityId` no cliente; o backend já isola.
- Nunca usar `municipalityId` de estado local para montar queries críticas; extrair do JWT/contexto.
- Nunca esconder rotas apenas via CSS; usar guards que impedem a renderização.

---

## 6. Client HTTP

```typescript
// shared/services/apiClient.ts

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Adiciona Bearer token em todas as requisições
apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Refresh automático em 401
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

## 7. Tipos TypeScript dos Domínios

```typescript
type UserRole =
  | 'SUPER_ADMIN' | 'ADMIN_OPERACIONAL' | 'GESTOR_PREFEITURA'
  | 'AGENTE_CAMPO' | 'VISUALIZADOR';

type RoadCondition = 'GOOD' | 'REGULAR' | 'BAD' | 'CRITICAL' | 'IMPASSABLE' | 'UNKNOWN';

type EvidenceStatus =
  | 'PENDING_UPLOAD' | 'UPLOADED' | 'PENDING_REVIEW'
  | 'APPROVED' | 'REJECTED' | 'DUPLICATED' | 'INVALID_LOCATION';

type OccurrenceStatus =
  | 'OPEN' | 'IN_ANALYSIS' | 'SCHEDULED' | 'IN_PROGRESS' | 'RESOLVED' | 'CANCELLED';

type MaintenanceStatus = 'PLANNED' | 'IN_PROGRESS' | 'FINISHED' | 'CANCELLED';

type ProblemType =
  | 'POTHOLES' | 'MUD' | 'FLOODING' | 'EROSION' | 'BRIDGE_DAMAGE'
  | 'VEGETATION' | 'BLOCKAGE' | 'DUST' | 'DRAINAGE_PROBLEM' | 'RUTTING' | 'OTHER';

interface RoadSegment {
  id: string;
  roadId: string;
  roadName: string;
  name: string;
  segmentOrder: number;
  geometry: GeoJSON.LineString;
  lengthMeters: number;
  currentCondition: RoadCondition;
  lastAssessmentAt: string | null;
  published: boolean;
}

interface InspectionEvidence {
  id: string;
  inspectionId: string;
  fieldAgentId: string;
  suggestedRoadSegmentId: string | null;
  confirmedRoadSegmentId: string | null;
  fileUrl: string;
  thumbnailUrl: string | null;
  latitude: number;
  longitude: number;
  gpsAccuracyMeters: number | null;
  takenAt: string;
  uploadedAt: string;
  reviewedAt: string | null;
  status: EvidenceStatus;
  fieldNote: string | null;
  adminNote: string | null;
}

interface RoadAssessment {
  id: string;
  roadSegmentId: string;
  evidenceId: string | null;
  condition: RoadCondition;
  severityScore: number;
  source: string;
  notes: string | null;
  assessedBy: string;
  assessedAt: string;
}

interface DashboardSummary {
  municipalityId: string;
  period: { startDate: string; endDate: string };
  totalMappedKm: number;
  kmByCondition: Record<RoadCondition, number>;
  repairedKm: number;
  openOccurrences: number;
  resolvedOccurrences: number;
}

interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  details?: { field: string; message: string }[];
}
```

---

## 8. Constantes Visuais

```typescript
// shared/constants/conditions.ts

export const CONDITION_COLORS: Record<RoadCondition, string> = {
  GOOD:       '#22c55e',
  REGULAR:    '#eab308',
  BAD:        '#f97316',
  CRITICAL:   '#ef4444',
  IMPASSABLE: '#7c3aed',
  UNKNOWN:    '#9ca3af',
};

export const CONDITION_LABELS: Record<RoadCondition, string> = {
  GOOD:       'Bom',
  REGULAR:    'Regular',
  BAD:        'Ruim',
  CRITICAL:   'Crítico',
  IMPASSABLE: 'Intransitável',
  UNKNOWN:    'Não avaliado',
};
```

---

## 9. Componentes Principais

### 9.1. Mapa

| Componente | Responsabilidade |
|---|---|
| `MapView` | Container do mapa, inicializa MapLibre, gerencia bounds |
| `RoadSegmentLayer` | Renderiza trechos como linhas coloridas por condição |
| `EvidenceMarkerLayer` | Marcadores de fotos (exibir só quando filtro ativo) |
| `ConditionLegend` | Legenda fixa no canto inferior direito |
| `MapDateFilter` | Seletor de data que recarrega cores dos trechos |
| `SegmentTooltip` | Tooltip ao passar o mouse sobre um trecho |
| `SegmentDetailsDrawer` | Painel lateral ao clicar em um trecho |

### 9.2. Dashboard

| Componente | Responsabilidade |
|---|---|
| `DashboardCards` | Cards com km por condição |
| `KmByConditionChart` | Gráfico de barras/donut com km por condição |
| `KmRepairedCard` | Card com km recuperados no período |
| `OccurrencesSummaryCard` | Card com ocorrências abertas/resolvidas |
| `DateRangeFilter` | Filtro de período aplicado ao dashboard inteiro |

### 9.3. Evidências

| Componente | Responsabilidade |
|---|---|
| `EvidenceReviewPanel` | Painel para aprovar/rejeitar/associar evidência |
| `EvidenceCard` | Miniatura + metadados para listagem |
| `EvidenceStatusBadge` | Badge colorido com status atual |
| `EvidenceLocationMap` | Mini mapa com pin da foto |
| `SegmentAssociationSelect` | Seletor de trecho para associar manualmente |

### 9.4. Trechos

| Componente | Responsabilidade |
|---|---|
| `SegmentDetailsDrawer` | Drawer com condição, extensão, histórico, fotos, ocorrências |
| `AssessmentHistoryList` | Lista de avaliações em ordem decrescente |
| `ConditionBadge` | Badge colorido com label da condição |
| `SegmentEvidenceGallery` | Grade de fotos aprovadas do trecho |

### 9.5. Compartilhados

| Componente | Responsabilidade |
|---|---|
| `PagedTable` | Tabela com paginação server-side |
| `ConfirmDialog` | Modal de confirmação para ações destrutivas |
| `StatusBadge` | Badge genérico por status com cor |
| `EmptyState` | Ilustração + texto para listas vazias |
| `LoadingOverlay` | Spinner para ações longas |

---

## 10. Regras de UI para o Mapa

- Trechos renderizados como linhas coloridas pela condição atual.
- Ao passar o mouse: tooltip com nome do trecho e condição.
- Ao clicar em um trecho: abrir `SegmentDetailsDrawer` lateralmente.
- Marcadores de evidências: visíveis apenas quando filtro de evidências estiver ativo.
- Trechos `UNKNOWN`: exibir em cinza.
- Legenda: fixa no canto inferior direito.
- Filtro por data: recarrega as cores sem recarregar a página.
- Trechos com `published = false` não aparecem no portal da prefeitura.

---

## 11. Fluxo de Revisão de Evidências (Admin)

```
1. Admin acessa /admin/evidences — lista com status PENDING_REVIEW
2. Abre EvidenceReviewPanel para uma evidência:
   a. Foto em tamanho real
   b. Coordenadas e precisão GPS
   c. Sugestão automática de trecho (se houver)
3. Ações disponíveis:
   a. Aprovar             → status APPROVED
   b. Rejeitar            → status REJECTED (motivo obrigatório)
   c. Marcar duplicada    → status DUPLICATED
   d. Localização inválida → status INVALID_LOCATION
   e. Associar a trecho   → preenche confirmed_road_segment_id
4. Após aprovação + associação, admin pode criar avaliação do trecho
```

---

## 12. Fluxo de Classificação de Trecho (Admin)

```
1. Admin acessa /admin/segments/:id
2. Vê condição atual e histórico de avaliações
3. Clica em "Nova Avaliação"
4. Preenche:
   - Condição (GOOD / REGULAR / BAD / CRITICAL / IMPASSABLE)
   - Severity Score (0–100, sugerido conforme condição escolhida)
   - Evidência base (opcional, de evidências aprovadas do trecho)
   - Observação
   - Data da avaliação
5. Confirma → backend atualiza current_condition do trecho
6. Dashboard reflete nova condição
```

---

## 13. Filtro Histórico do Mapa

1. Usuário seleciona data no `MapDateFilter`.
2. Frontend envia `?date=2026-03-15` para `GET /dashboard/map-segments`.
3. Backend retorna trechos com a condição válida naquela data.
4. `RoadSegmentLayer` re-renderiza as linhas com as novas cores.
5. Cards do dashboard atualizam com os valores do período.

O frontend não calcula condição histórica. Exibe o que a API retorna.

---

## 14. Consumo das APIs

| Feature | Endpoints principais |
|---|---|
| Auth | `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me` |
| Dashboard | `GET /dashboard/summary`, `GET /dashboard/km-by-condition`, `GET /dashboard/map-segments` |
| Trechos | `GET /road-segments`, `POST /road-segments`, `GET /road-segments/:id`, `GET /road-segments/:id/history` |
| Evidências | `GET /evidences`, `POST /evidences/upload`, `PATCH /evidences/:id/approve`, `PATCH /evidences/:id/associate-segment` |
| Avaliações | `POST /assessments`, `GET /road-segments/:id/assessments` |
| Ocorrências | `GET /occurrences`, `POST /occurrences`, `PATCH /occurrences/:id/status` |
| Manutenção | `GET /maintenance-events`, `POST /maintenance-events`, `PATCH /maintenance-events/:id/finish` |
| Relatórios | `GET /reports/summary.csv`, `GET /reports/summary.pdf` |

Paginação padrão: `?page=0&size=20&sort=createdAt,desc`

---

## 15. Permissões por Papel — Guards de Tela

| Tela / Ação | SUPER_ADMIN | ADMIN_OPERACIONAL | GESTOR_PREFEITURA | VISUALIZADOR |
|---|:---:|:---:|:---:|:---:|
| Dashboard admin | Sim | Sim | — | — |
| Dashboard prefeitura | Sim | Sim | Sim | Sim |
| Mapa colorido | Sim | Sim | Sim | Sim |
| Revisar evidências | Sim | Sim | Opcional | — |
| Classificar trecho | Sim | Sim | Opcional | — |
| Gerenciar usuários | Sim | Sim | — | — |
| Gerenciar municípios | Sim | — | — | — |
| Criar/editar estrada | Sim | Sim | — | — |
| Criar/editar trecho | Sim | Sim | — | — |
| Registrar manutenção | Sim | Sim | Opcional | — |
| Exportar relatório | Sim | Sim | Sim | Opcional |

---

## 16. Variáveis de Ambiente

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_MAP_PROVIDER=maplibre
VITE_MAP_STYLE_URL=https://demotiles.maplibre.org/style.json
```

---

## 17. Testes

Priorizar:

- Renderização do mapa com trechos coloridos por condição.
- Guard de rota: usuário com papel incorreto é redirecionado.
- Fluxo de login e armazenamento do token em memória.
- Refresh automático do token após resposta 401.
- Cards do dashboard exibem valores corretos da API.
- Submissão do formulário de avaliação atualiza estado local.
- Filtro de data atualiza cores do mapa e valores dos cards.

---

## 18. Ordem de Implementação

1. Configurar Vite + React + TypeScript + roteamento.
2. Tela de login + AuthContext + apiClient com interceptors.
3. Layout base admin (sidebar, header, outlet).
4. Dashboard com cards de km por condição (dados reais).
5. Mapa com trechos coloridos (MapLibre + endpoint `map-segments`).
6. Legenda fixa e drawer de detalhe do trecho.
7. Listagem de evidências pendentes de revisão.
8. Painel de revisão de evidência (aprovar/rejeitar/associar).
9. Formulário de nova avaliação de trecho.
10. Histórico de avaliações do trecho.
11. CRUD de ocorrências.
12. CRUD de intervenções.
13. Filtro temporal do dashboard e mapa.
14. Portal da prefeitura (dashboard + mapa somente leitura).
15. Exportação de relatório CSV e PDF.

---

## 19. Definition of Done (Frontend Web)

- [ ] Tela renderiza sem erros em desktop e tablet.
- [ ] Dados vêm da API (sem mock hardcoded em produção).
- [ ] Loading state exibido durante requisições.
- [ ] Mensagem de erro exibida ao usuário em caso de falha.
- [ ] Guards de permissão implementados para a tela/ação.
- [ ] Fluxo principal testado manualmente com backend rodando.
- [ ] Tipos TypeScript sem `any` nas interfaces de domínio.
- [ ] `municipalityId` extraído do token/contexto, nunca hardcoded.
