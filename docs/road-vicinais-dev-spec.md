# Especificação Técnica e Regras de Negócio — Plataforma de Monitoramento de Estradas Vicinais

> Documento base para desenvolvimento assistido por IA.  
> Este arquivo deve ser usado como fonte de verdade inicial para construção do backend, frontend web, aplicativo mobile, banco de dados, APIs, regras de negócio e fluxos do produto.

---

## 1. Visão Geral do Produto

### 1.1. Nome conceitual

**Plataforma de Monitoramento de Estradas Vicinais**

Nomes internos possíveis:

- `Estradas Vicinais`
- `Mapa Vicinal`
- `Rota Rural`
- `Vicinal Monitor`
- `SGEV` — Sistema de Gestão de Estradas Vicinais

Para fins de desenvolvimento, usar o nome neutro:

```txt
sgev
```

---

### 1.2. Objetivo do software

Criar uma plataforma para mapeamento, monitoramento e gestão da condição de estradas vicinais, também chamadas de estradas carroçais, rurais ou não pavimentadas.

O sistema deve permitir que agentes de campo coletem fotos georreferenciadas dos trechos das estradas, mesmo sem internet, e que essas evidências sejam posteriormente analisadas por administradores para compor um mapa visual da saúde da malha vicinal do município.

O cliente final, normalmente uma prefeitura, deve conseguir visualizar em um painel web:

- quais estradas estão boas;
- quais estradas estão regulares;
- quais estradas estão ruins;
- quais estradas estão críticas;
- quais trechos estão intransitáveis;
- quantos quilômetros existem em cada situação;
- quantos quilômetros precisam de manutenção;
- quantos quilômetros já foram recuperados;
- fotos comprobatórias dos trechos;
- histórico da evolução das condições ao longo do tempo.

---

### 1.3. Proposta de valor

A plataforma transforma dados coletados em campo em informação gerencial para a prefeitura.

A foto isolada não é o produto final. O produto final é:

- mapa georreferenciado;
- histórico auditável;
- cálculo de quilômetros por condição;
- priorização de manutenção;
- prestação de contas;
- evidências antes/depois;
- relatórios de execução.

Frase de produto:

```txt
A prefeitura passa a ter um mapa vivo das estradas vicinais, com fotos, localização, histórico e cálculo de quilômetros por situação, permitindo priorizar manutenção, justificar uso de máquinas, prestar contas à população e comprovar quantos quilômetros foram recuperados.
```

---

## 2. Escopo do MVP

### 2.1. MVP inicial

O MVP deve ser manual e auditável. A inteligência artificial não é obrigatória no primeiro momento.

O MVP deve conter:

1. Cadastro de municípios.
2. Cadastro de usuários.
3. Cadastro/importação/desenho de estradas vicinais.
4. Divisão das estradas em trechos.
5. Aplicativo mobile para coleta offline de fotos com GPS.
6. Sincronização das fotos quando houver internet.
7. Painel administrativo para revisão das evidências.
8. Classificação manual da condição dos trechos.
9. Portal web para a prefeitura visualizar mapa e indicadores.
10. Dashboard com quilômetros por condição.
11. Histórico de avaliações.
12. Registro de manutenção/intervenção.
13. Relatório básico em PDF/CSV/Excel.

---

### 2.2. Fora do escopo do MVP

Não implementar no MVP inicial, salvo decisão explícita:

- classificação automática por IA;
- detecção automática de buracos;
- integração com sistemas oficiais da prefeitura;
- roteirização automática do agente de campo;
- cobrança financeira dentro da plataforma;
- app para cidadão comum reportar problemas;
- notificações push complexas;
- machine learning treinado com dataset próprio;
- análise de vídeo em tempo real;
- acompanhamento em tempo real do agente no mapa.

---

### 2.3. Evolução futura

Após o MVP, o produto pode evoluir para:

- IA sugerindo classificação da estrada;
- detecção de buracos, lama, erosão, alagamento e ponte danificada;
- comparação antes/depois por imagem;
- priorização automática de manutenção;
- integração com frota de máquinas;
- app público para denúncias da população;
- importação de dados GIS oficiais;
- mapas offline completos;
- trilhas GPS contínuas do percurso;
- monitoramento por vídeo;
- relatórios executivos com indicadores políticos e administrativos.

---

## 3. Perfis de Usuário

### 3.1. Agente de Campo

Usuário que vai presencialmente até as estradas e coleta evidências.

No vocabulário informal, pode ser chamado de motoqueiro, mas no sistema deve ser chamado de:

```txt
Agente de Campo
```

Responsabilidades:

- acessar o app mobile;
- tirar fotos dos trechos;
- permitir captura de GPS;
- registrar observações;
- trabalhar offline;
- sincronizar dados quando houver internet;
- consultar coletas pendentes;
- reenviar coletas com falha.

O agente de campo não deve ter permissão para alterar dados oficiais do mapa da prefeitura.

---

### 3.2. Administrador Operacional

Usuário da equipe interna ou da equipe gestora do sistema.

Responsabilidades:

- gerenciar municípios;
- cadastrar estradas;
- importar ou desenhar geometrias;
- revisar fotos recebidas;
- aprovar ou rejeitar evidências;
- associar fotos a trechos;
- classificar condição dos trechos;
- abrir ocorrências;
- registrar manutenção;
- publicar informações para o portal da prefeitura;
- corrigir inconsistências.

---

### 3.3. Gestor da Prefeitura

Usuário final, cliente da plataforma.

Responsabilidades:

- visualizar dashboard;
- consultar mapa colorido;
- acessar evidências;
- acompanhar histórico;
- acompanhar manutenção;
- extrair relatórios;
- visualizar indicadores consolidados.

O gestor da prefeitura não deve, no MVP, ter permissão para alterar diretamente as classificações oficiais, salvo se configurado como usuário administrativo.

---

### 3.4. Super Administrador

Usuário interno da empresa dona da plataforma.

Responsabilidades:

- gerenciar todos os municípios;
- ativar/desativar clientes;
- gerenciar planos e permissões;
- auditar logs;
- acessar dados multi-tenant quando permitido;
- configurar parâmetros globais.

---

## 4. Modelo de Permissões

### 4.1. Papéis do sistema

Usar enum:

```txt
SUPER_ADMIN
ADMIN_OPERACIONAL
GESTOR_PREFEITURA
AGENTE_CAMPO
VISUALIZADOR
```

---

### 4.2. Permissões por papel

| Permissão | SUPER_ADMIN | ADMIN_OPERACIONAL | GESTOR_PREFEITURA | AGENTE_CAMPO | VISUALIZADOR |
|---|---:|---:|---:|---:|---:|
| Criar município | Sim | Não | Não | Não | Não |
| Editar município | Sim | Limitado | Não | Não | Não |
| Cadastrar usuários | Sim | Sim | Opcional | Não | Não |
| Criar estrada | Sim | Sim | Não no MVP | Não | Não |
| Editar estrada | Sim | Sim | Não no MVP | Não | Não |
| Criar trecho | Sim | Sim | Não no MVP | Não | Não |
| Coletar fotos | Não usual | Sim | Não usual | Sim | Não |
| Sincronizar fotos | Não usual | Sim | Não usual | Sim | Não |
| Revisar evidências | Sim | Sim | Opcional | Não | Não |
| Classificar trecho | Sim | Sim | Opcional | Não | Não |
| Ver dashboard | Sim | Sim | Sim | Opcional | Sim |
| Exportar relatório | Sim | Sim | Sim | Não | Opcional |
| Registrar manutenção | Sim | Sim | Opcional | Não | Não |
| Ver auditoria | Sim | Opcional | Não | Não | Não |

---

## 5. Conceitos de Domínio

### 5.1. Município

Representa uma prefeitura cliente.

Cada município possui:

- nome;
- UF;
- código IBGE opcional;
- geometria do limite municipal opcional;
- usuários vinculados;
- estradas vinculadas;
- configurações próprias.

O sistema deve ser multi-tenant por município.

---

### 5.2. Estrada

Representa uma estrada vicinal completa.

Exemplo:

```txt
Estrada do Sítio Lagoa Nova
```

A estrada pode ser composta por vários trechos.

Campos principais:

- nome;
- descrição;
- município;
- geometria principal;
- extensão total;
- situação agregada;
- ativa/inativa.

---

### 5.3. Trecho de Estrada

É a unidade central do sistema.

Um trecho representa uma parte delimitada da estrada.

Exemplo:

```txt
Estrada Lagoa Nova — Trecho 01 — km 0,0 ao km 2,4
```

Cada trecho possui:

- geometria do tipo linha;
- extensão em metros;
- status atual;
- histórico de avaliações;
- fotos/evidências vinculadas;
- ocorrências;
- manutenções.

O dashboard deve calcular quilômetros por situação com base nos trechos.

---

### 5.4. Vistoria

Representa uma atividade de coleta feita por um agente de campo.

Uma vistoria pode conter várias fotos.

Exemplo:

```txt
Vistoria realizada por João em 21/06/2026 na região de Lagoa Nova.
```

Campos:

- agente responsável;
- município;
- data/hora de início;
- data/hora de término;
- status de sincronização;
- observações;
- fotos coletadas.

---

### 5.5. Evidência

Representa uma foto ou arquivo coletado em campo.

No MVP, evidência será principalmente foto.

Cada evidência deve possuir:

- URL da imagem original;
- URL da miniatura;
- latitude;
- longitude;
- precisão do GPS;
- data/hora da captura;
- data/hora do upload;
- agente responsável;
- vistoria vinculada;
- trecho sugerido;
- trecho confirmado;
- status de revisão;
- observação.

---

### 5.6. Avaliação de Trecho

Representa a classificação oficial de um trecho em determinado momento.

Exemplo:

```txt
Trecho 03 classificado como CRÍTICO em 21/06/2026.
```

Campos:

- trecho;
- condição;
- severidade numérica;
- data da avaliação;
- avaliador;
- evidência base;
- observação;
- origem da avaliação.

A avaliação não deve sobrescrever o histórico. Toda mudança deve gerar novo registro.

---

### 5.7. Ocorrência

Representa um problema aberto em determinado trecho.

Exemplos:

- buracos profundos;
- lama;
- alagamento;
- erosão;
- ponte danificada;
- estrada intransitável;
- vegetação invadindo a via;
- atoleiro;
- passagem interrompida.

---

### 5.8. Intervenção ou Manutenção

Representa uma ação da prefeitura para recuperar um trecho.

Exemplos:

- patrolamento;
- piçarramento;
- cascalhamento;
- drenagem;
- construção de bueiro;
- recuperação de ponte;
- retirada de barreira;
- limpeza lateral;
- compactação.

A intervenção deve permitir cálculo de quilômetros recuperados.

---

## 6. Estados e Enums

### 6.1. Condição do trecho

Usar enum:

```txt
GOOD
REGULAR
BAD
CRITICAL
IMPASSABLE
UNKNOWN
```

Tradução visual:

| Enum | Label | Cor sugerida | Significado |
|---|---|---|---|
| GOOD | Bom | Verde | Transitável sem necessidade imediata |
| REGULAR | Regular | Amarelo | Transitável com atenção |
| BAD | Ruim | Laranja | Necessita manutenção |
| CRITICAL | Crítico | Vermelho | Alta prioridade de manutenção |
| IMPASSABLE | Intransitável | Preto/Roxo | Passagem bloqueada ou muito insegura |
| UNKNOWN | Não avaliado | Cinza | Sem avaliação recente |

---

### 6.2. Status da evidência

```txt
PENDING_UPLOAD
UPLOADED
PENDING_REVIEW
APPROVED
REJECTED
DUPLICATED
INVALID_LOCATION
```

Significado:

- `PENDING_UPLOAD`: foto ainda está apenas no celular.
- `UPLOADED`: arquivo enviado ao servidor.
- `PENDING_REVIEW`: aguardando análise do admin.
- `APPROVED`: evidência válida.
- `REJECTED`: evidência rejeitada.
- `DUPLICATED`: foto duplicada.
- `INVALID_LOCATION`: localização incompatível ou ausente.

---

### 6.3. Status da vistoria

```txt
DRAFT
IN_PROGRESS
PENDING_SYNC
SYNCED
SYNC_ERROR
CLOSED
CANCELLED
```

---

### 6.4. Status da ocorrência

```txt
OPEN
IN_ANALYSIS
SCHEDULED
IN_PROGRESS
RESOLVED
CANCELLED
```

---

### 6.5. Status da intervenção

```txt
PLANNED
IN_PROGRESS
FINISHED
CANCELLED
```

---

### 6.6. Tipo de problema

```txt
POTHOLES
MUD
FLOODING
EROSION
BRIDGE_DAMAGE
VEGETATION
BLOCKAGE
DUST
DRAINAGE_PROBLEM
RUTTING
OTHER
```

Labels:

- `POTHOLES`: buracos;
- `MUD`: lama;
- `FLOODING`: alagamento;
- `EROSION`: erosão;
- `BRIDGE_DAMAGE`: ponte danificada;
- `VEGETATION`: vegetação invadindo;
- `BLOCKAGE`: bloqueio;
- `DUST`: poeira excessiva;
- `DRAINAGE_PROBLEM`: problema de drenagem;
- `RUTTING`: trilhas/deformação por roda;
- `OTHER`: outros.

---

## 7. Regras de Negócio

### 7.1. Regras gerais

#### RN-001 — Multi-tenancy por município

Todo dado operacional deve pertencer a um município.

Entidades obrigatoriamente vinculadas a `municipality_id`:

- usuários, exceto super admin;
- estradas;
- trechos;
- vistorias;
- evidências;
- avaliações;
- ocorrências;
- intervenções;
- relatórios.

Um usuário não-super-admin não deve acessar dados de outro município.

---

#### RN-002 — Trecho é a unidade oficial de cálculo

O sistema deve calcular indicadores de quilômetros usando trechos de estrada, não fotos isoladas.

Fotos são evidências. Trechos são unidades de gestão.

---

#### RN-003 — Histórico não deve ser apagado

Alterações de condição de estrada devem gerar novas avaliações.

Não sobrescrever avaliações antigas.

A tabela de avaliações deve permitir reconstruir o estado do mapa em datas passadas.

---

#### RN-004 — Status atual do trecho

O status atual de um trecho deve ser derivado da avaliação válida mais recente.

Pode ser armazenado também em `road_segments.current_condition` por performance, mas a fonte histórica deve ser `road_assessments`.

---

#### RN-005 — Foto não define sozinha a condição oficial

No MVP, uma evidência não deve alterar automaticamente o status do trecho.

Fluxo correto:

1. foto é enviada;
2. admin revisa;
3. admin aprova;
4. admin associa a um trecho;
5. admin cria avaliação;
6. avaliação altera status atual do trecho.

---

#### RN-006 — Evidência rejeitada não entra no dashboard

Fotos rejeitadas não devem afetar:

- condição do trecho;
- indicadores;
- relatórios;
- histórico oficial;
- cálculo de quilômetros.

Devem permanecer disponíveis apenas para auditoria.

---

#### RN-007 — Localização é obrigatória para evidência oficial

Toda evidência oficial deve ter:

- latitude;
- longitude;
- data/hora de captura.

Se não houver localização, a evidência pode ser salva, mas deve ficar com status `INVALID_LOCATION` ou `PENDING_REVIEW`, dependendo da configuração.

---

#### RN-008 — Precisão do GPS deve ser armazenada

Sempre que disponível, o app deve armazenar `gps_accuracy_meters`.

Se a precisão for ruim, o sistema deve alertar o usuário.

Parâmetros sugeridos:

- até 10 m: excelente;
- 10 a 30 m: aceitável;
- 30 a 50 m: atenção;
- acima de 50 m: ruim.

No MVP, não bloquear coleta por baixa precisão, mas exibir alerta.

---

#### RN-009 — Upload pode ocorrer depois da coleta

O agente de campo deve conseguir coletar fotos sem internet.

As fotos ficam em fila local até a sincronização.

O app deve preservar metadados mesmo se for fechado.

---

#### RN-010 — Data da captura é diferente da data do upload

O sistema deve armazenar separadamente:

- `taken_at`: momento em que a foto foi capturada;
- `uploaded_at`: momento em que a foto foi enviada ao servidor;
- `reviewed_at`: momento em que foi revisada;
- `assessed_at`: momento da avaliação do trecho.

Relatórios históricos devem usar preferencialmente `assessed_at` para status oficial e `taken_at` para evidência de campo.

---

#### RN-011 — Cálculo de quilômetros por condição

Para calcular km por condição:

1. filtrar trechos ativos do município;
2. obter a condição atual ou a condição válida na data selecionada;
3. somar `length_meters` dos trechos por condição;
4. converter metros para quilômetros dividindo por 1000;
5. arredondar para duas casas decimais na apresentação.

Exemplo:

```txt
BAD = soma(length_meters onde condição = BAD) / 1000
```

---

#### RN-012 — Filtro histórico do mapa

Quando o usuário escolher uma data passada, o sistema deve reconstruir o estado de cada trecho com base na avaliação mais recente até aquela data.

Exemplo:

```txt
Data selecionada: 15/03/2026
Para cada trecho, buscar a última avaliação com assessed_at <= 15/03/2026 23:59:59
```

Se não houver avaliação até a data, exibir como `UNKNOWN`.

---

#### RN-013 — Intervenção concluída pode gerar nova avaliação

Ao concluir uma intervenção, o sistema deve permitir registrar uma nova avaliação do trecho.

Exemplo:

- antes: `CRITICAL`;
- intervenção: patrolamento;
- depois: `GOOD`.

Essa nova avaliação deve alimentar os indicadores de quilômetros recuperados.

---

#### RN-014 — Quilômetros recuperados

Quilômetros recuperados devem ser calculados com base em intervenções finalizadas.

Regra inicial:

```txt
km_recuperados = soma(repaired_length_meters de intervenções FINISHED no período) / 1000
```

Regra alternativa futura:

```txt
km_recuperados = soma(length_meters dos trechos que mudaram de BAD/CRITICAL/IMPASSABLE para GOOD/REGULAR após intervenção)
```

No MVP, usar `repaired_length_meters` informado no registro da intervenção.

---

#### RN-015 — Uma foto pode estar próxima de vários trechos

O sistema pode sugerir automaticamente o trecho mais próximo da foto, mas o admin deve confirmar.

Critério sugerido:

```txt
Buscar road_segment mais próximo da coordenada da foto dentro de um raio configurável, inicialmente 100 metros.
```

Se nenhum trecho for encontrado no raio, a evidência fica como não associada.

---

#### RN-016 — Trecho pode ter múltiplas evidências

Um trecho pode possuir várias fotos, em diferentes vistorias e datas.

A tela do trecho deve permitir visualizar evidências por ordem cronológica.

---

#### RN-017 — Evidência deve preservar autoria

Toda evidência deve armazenar o usuário que coletou a foto.

Não permitir alterar `field_agent_id` depois da sincronização, salvo por super admin com auditoria.

---

#### RN-018 — Auditoria obrigatória em ações críticas

Ações que devem gerar log de auditoria:

- login;
- criação/edição/exclusão de estrada;
- criação/edição/exclusão de trecho;
- aprovação/rejeição de evidência;
- criação de avaliação;
- alteração de condição atual;
- criação/edição/conclusão de intervenção;
- exportação de relatório;
- alteração de permissões.

---

#### RN-019 — Exclusão lógica

Entidades principais não devem ser excluídas fisicamente no MVP.

Usar campos:

```txt
active boolean
created_at
updated_at
deleted_at nullable
deleted_by nullable
```

---

#### RN-020 — Classificação deve seguir critérios objetivos

Critérios recomendados:

| Condição | Critério operacional |
|---|---|
| GOOD | Trânsito normal, sem necessidade imediata de manutenção |
| REGULAR | Trânsito possível, mas com irregularidades leves |
| BAD | Trânsito lento ou desconfortável, manutenção recomendada |
| CRITICAL | Trânsito difícil, risco de atolamento ou dano ao veículo |
| IMPASSABLE | Trecho bloqueado, alagado, destruído ou sem passagem segura |
| UNKNOWN | Sem avaliação válida |

---

#### RN-021 — Classificação manual prevalece sobre sugestão automática

Quando houver IA no futuro, a classificação manual aprovada pelo admin deve prevalecer.

---

#### RN-022 — Relatórios devem indicar período

Todo relatório exportado deve mostrar:

- município;
- período analisado;
- data/hora de geração;
- usuário que gerou;
- filtros utilizados;
- totais de km por condição;
- lista de trechos relevantes;
- evidências quando aplicável.

---

#### RN-023 — Foto deve ter miniatura

Ao receber uma foto, o backend ou um worker deve gerar uma versão reduzida para listagens.

Campos:

- `file_url`: imagem original;
- `thumbnail_url`: miniatura.

---

#### RN-024 — O app deve evitar perda de dados

Antes de apagar uma foto local, o app deve confirmar que:

1. upload foi concluído;
2. servidor retornou sucesso;
3. o registro remoto foi criado;
4. o ID remoto foi salvo localmente.

---

#### RN-025 — Sincronização deve ser idempotente

Se o app tentar enviar a mesma evidência mais de uma vez, o backend não deve criar duplicatas.

Usar um identificador local único:

```txt
client_uuid
```

O backend deve rejeitar ou retornar o registro existente se receber o mesmo `client_uuid` para o mesmo usuário.

---

#### RN-026 — Geometria do trecho deve ser validada

Um trecho deve possuir geometria válida.

Não permitir trecho sem geometria oficial se ele for usado em dashboard.

---

#### RN-027 — Comprimento do trecho

O comprimento do trecho deve ser calculado automaticamente a partir da geometria sempre que possível.

Permitir ajuste manual apenas com permissão administrativa e campo de justificativa.

---

#### RN-028 — Mapa deve exibir apenas dados publicados para prefeitura

No MVP, pode existir separação entre dados internos e publicados.

Campos sugeridos:

```txt
published boolean
published_at timestamp nullable
```

Se `published = false`, a prefeitura não visualiza o trecho/avaliação no portal final.

---

#### RN-029 — Ocorrência resolvida não apaga histórico

Ao resolver uma ocorrência, ela deve ficar disponível no histórico.

Não remover do banco.

---

#### RN-030 — Dados do município devem ser isolados

Nunca retornar dados de outro município em endpoints de usuário comum, mesmo que o ID seja informado manualmente na URL.

---

## 8. Requisitos Funcionais

### 8.1. Autenticação

#### RF-AUTH-001 — Login

O sistema deve permitir login por e-mail e senha.

Entrada:

```json
{
  "email": "usuario@municipio.gov.br",
  "password": "senha"
}
```

Saída:

```json
{
  "accessToken": "jwt",
  "refreshToken": "jwt",
  "user": {
    "id": "uuid",
    "name": "Nome",
    "email": "usuario@municipio.gov.br",
    "role": "GESTOR_PREFEITURA",
    "municipalityId": "uuid"
  }
}
```

---

#### RF-AUTH-002 — Refresh token

O sistema deve permitir renovar token sem novo login.

---

#### RF-AUTH-003 — Logout

O sistema deve permitir invalidar refresh token.

---

### 8.2. Municípios

#### RF-MUN-001 — Criar município

Campos:

- nome;
- UF;
- código IBGE;
- geometria do limite municipal opcional;
- ativo.

---

#### RF-MUN-002 — Listar municípios

Apenas super admin deve listar todos.

Usuários comuns visualizam apenas o próprio município.

---

### 8.3. Estradas

#### RF-ROAD-001 — Criar estrada

Campos:

- município;
- nome;
- descrição;
- geometria opcional;
- ativa.

---

#### RF-ROAD-002 — Importar estrada

O sistema deve futuramente permitir importação por:

- GeoJSON;
- KML;
- Shapefile;
- desenho manual no mapa.

Para o MVP, priorizar GeoJSON e desenho manual.

---

#### RF-ROAD-003 — Listar estradas no mapa

O portal deve retornar estradas e trechos com geometria e condição atual.

---

### 8.4. Trechos

#### RF-SEG-001 — Criar trecho

Campos:

- estrada;
- nome;
- ordem;
- geometria LineString;
- comprimento calculado;
- condição inicial `UNKNOWN`.

---

#### RF-SEG-002 — Editar trecho

Permitir alterar:

- nome;
- geometria;
- ordem;
- ativo/inativo.

Ao alterar geometria, recalcular comprimento.

---

#### RF-SEG-003 — Consultar detalhes do trecho

A tela de detalhe deve exibir:

- nome da estrada;
- nome do trecho;
- condição atual;
- extensão;
- última avaliação;
- fotos recentes;
- histórico de avaliações;
- ocorrências abertas;
- intervenções realizadas.

---

### 8.5. Vistorias Mobile

#### RF-INSP-001 — Criar vistoria local

O app mobile deve permitir iniciar uma vistoria mesmo sem internet.

Campos locais:

- `client_uuid`;
- agente;
- município;
- started_at;
- status local.

---

#### RF-INSP-002 — Capturar foto

O app deve permitir tirar foto e salvar:

- imagem;
- latitude;
- longitude;
- precisão;
- data/hora;
- observação opcional;
- orientação do dispositivo opcional;
- `client_uuid`.

---

#### RF-INSP-003 — Listar pendências de sincronização

O agente deve visualizar quantas fotos ainda não foram enviadas.

---

#### RF-INSP-004 — Sincronizar vistoria

Quando houver internet, o app deve enviar:

- metadados da vistoria;
- metadados das fotos;
- arquivos das fotos.

---

### 8.6. Evidências

#### RF-EVD-001 — Receber upload de evidência

O backend deve receber a imagem e metadados.

---

#### RF-EVD-002 — Sugerir trecho próximo

Após receber a evidência, o backend deve tentar sugerir o trecho mais próximo.

---

#### RF-EVD-003 — Revisar evidência

Admin deve poder:

- aprovar;
- rejeitar;
- marcar como duplicada;
- marcar como localização inválida;
- associar a trecho;
- alterar observação administrativa.

---

### 8.7. Avaliações

#### RF-ASS-001 — Criar avaliação de trecho

Admin deve poder registrar:

- trecho;
- condição;
- severidade;
- observação;
- evidência base;
- data de avaliação.

---

#### RF-ASS-002 — Histórico de avaliações

Sistema deve listar avaliações de um trecho em ordem decrescente.

---

### 8.8. Ocorrências

#### RF-OCC-001 — Criar ocorrência

Campos:

- trecho;
- tipo de problema;
- descrição;
- severidade;
- status;
- evidências vinculadas.

---

#### RF-OCC-002 — Atualizar ocorrência

Permitir alterar status:

- aberta;
- em análise;
- programada;
- em execução;
- resolvida;
- cancelada.

---

### 8.9. Intervenções

#### RF-MNT-001 — Registrar intervenção

Campos:

- trecho;
- tipo de intervenção;
- status;
- data início;
- data fim;
- comprimento recuperado;
- observação.

---

#### RF-MNT-002 — Concluir intervenção

Ao concluir, solicitar:

- data de conclusão;
- km recuperados;
- evidência pós-manutenção opcional;
- nova avaliação opcional.

---

### 8.10. Dashboard

#### RF-DASH-001 — Indicadores gerais

O dashboard deve exibir:

- total de km mapeados;
- km bons;
- km regulares;
- km ruins;
- km críticos;
- km intransitáveis;
- km sem avaliação;
- km recuperados no período;
- ocorrências abertas;
- ocorrências resolvidas.

---

#### RF-DASH-002 — Mapa colorido

O mapa deve exibir trechos coloridos pela condição atual ou pela condição na data filtrada.

---

#### RF-DASH-003 — Filtro temporal

Permitir filtrar por:

- data específica;
- intervalo de datas;
- mês;
- ano.

---

#### RF-DASH-004 — Filtros adicionais

Permitir filtrar por:

- estrada;
- localidade;
- condição;
- tipo de problema;
- status da ocorrência;
- data da última vistoria.

---

### 8.11. Relatórios

#### RF-REP-001 — Exportar relatório

Formatos desejados:

- PDF;
- CSV;
- XLSX.

No MVP, priorizar CSV e PDF simples.

---

#### RF-REP-002 — Conteúdo mínimo do relatório

O relatório deve conter:

- município;
- período;
- resumo de km por condição;
- lista de trechos críticos;
- fotos principais;
- intervenções concluídas;
- km recuperados;
- data de geração.

---

## 9. Requisitos Não Funcionais

### 9.1. Performance

- Listagem do mapa deve responder em até 3 segundos para municípios pequenos/médios.
- Dashboard deve responder em até 5 segundos.
- Upload de fotos pode ser assíncrono.
- Gerar miniaturas de forma assíncrona se necessário.

---

### 9.2. Escalabilidade

A arquitetura deve suportar múltiplos municípios.

Evitar acoplamento rígido a um único cliente.

---

### 9.3. Offline-first

O app mobile deve ser projetado para funcionar sem internet.

Dados mínimos offline:

- usuário autenticado previamente;
- município vinculado;
- vistorias locais;
- fotos pendentes;
- fila de sincronização;
- logs locais de erro.

---

### 9.4. Segurança

- Usar HTTPS em produção.
- Usar JWT com expiração.
- Usar refresh token seguro.
- Senhas com hash forte.
- Validar permissão por município.
- Não expor URLs privadas sem controle.
- Assinar URLs de imagem quando necessário.

---

### 9.5. Auditoria

Ações críticas devem ser auditadas.

Tabela sugerida:

```txt
audit_logs
```

Campos:

- usuário;
- ação;
- entidade;
- ID da entidade;
- valores anteriores;
- valores novos;
- IP;
- user agent;
- data/hora.

---

### 9.6. Observabilidade

O backend deve possuir logs estruturados.

Registrar:

- erros de upload;
- erros de sincronização;
- erros de autenticação;
- operações lentas;
- falhas em cálculos geográficos.

---

## 10. Arquitetura Técnica

### 10.1. Stack principal

```txt
Backend: Java 21 + Spring Boot
Frontend Web: React + TypeScript
Mobile: React Native + TypeScript
Banco: PostgreSQL + PostGIS
Storage de imagens: S3, MinIO ou compatível
Mapas: MapLibre, Leaflet ou Google Maps
Autenticação: JWT + Refresh Token
Infra local: Docker Compose
```

---

### 10.2. Decisão de banco

Usar PostgreSQL com extensão PostGIS.

Motivos:

- armazenar geometria de estradas e trechos;
- calcular distância entre foto e trecho;
- calcular comprimento de trechos;
- fazer filtros geográficos;
- suportar consultas espaciais.

---

### 10.3. Armazenamento de fotos

Não salvar binário da foto no banco.

Salvar arquivo em storage externo:

- AWS S3;
- MinIO;
- Cloudflare R2;
- Google Cloud Storage;
- outro compatível.

No banco, salvar apenas:

- URL;
- key/path;
- tamanho;
- mime type;
- hash;
- metadados.

---

### 10.4. Arquitetura geral

```txt
[React Native App]
        |
        | HTTPS / sync
        v
[Spring Boot API]
        |
        | SQL + Spatial Queries
        v
[PostgreSQL + PostGIS]
        |
        | file metadata
        v
[Object Storage: S3/MinIO]

[React Web Admin]
        |
        v
[Spring Boot API]

[React Portal Prefeitura]
        |
        v
[Spring Boot API]
```

---

### 10.5. Módulos do backend

Sugestão de módulos por domínio:

```txt
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

---

### 10.6. Estrutura sugerida do repositório

Monorepo:

```txt
sgev/
├── README.md
├── docs/
│   ├── development-spec.md
│   ├── api.md
│   ├── database.md
│   ├── mobile-offline-sync.md
│   └── business-rules.md
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
├── web/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
├── mobile/
│   ├── package.json
│   ├── app.json
│   └── src/
├── infra/
│   ├── docker-compose.yml
│   ├── postgres/
│   ├── minio/
│   └── nginx/
└── scripts/
    ├── seed-dev.sql
    └── reset-local.sh
```

---

## 11. Backend — Organização Interna

### 11.1. Padrão de camadas

Usar estrutura por feature/domínio.

Exemplo para `roadsegments`:

```txt
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

---

### 11.2. Regras de código backend

- Controller não deve conter regra de negócio.
- Service concentra regra de negócio.
- Repository apenas persistência.
- DTOs não devem expor entidade diretamente.
- Usar validação com Bean Validation.
- Usar transações em casos de escrita.
- Usar exceptions de domínio.
- Todo endpoint deve validar município/permissão.

---

### 11.3. Pacotes essenciais

```txt
auth
users
municipalities
roads
roadsegments
inspections
evidences
assessments
occurrences
maintenance
dashboard
reports
storage
audit
```

---

## 12. Frontend Web

### 12.1. Aplicações web

Pode ser uma única aplicação React com roteamento por perfil.

Rotas sugeridas:

```txt
/login
/admin/dashboard
/admin/municipalities
/admin/users
/admin/roads
/admin/segments
/admin/evidences
/admin/assessments
/admin/occurrences
/admin/maintenance
/admin/reports

/prefeitura/dashboard
/prefeitura/mapa
/prefeitura/trechos/:id
/prefeitura/relatorios
```

---

### 12.2. Estrutura do frontend

```txt
web/src/
├── app
│   ├── router.tsx
│   ├── providers.tsx
│   └── guards.tsx
├── shared
│   ├── components
│   ├── hooks
│   ├── services
│   ├── types
│   ├── utils
│   └── constants
├── features
│   ├── auth
│   ├── dashboard
│   ├── map
│   ├── roads
│   ├── segments
│   ├── evidences
│   ├── occurrences
│   ├── maintenance
│   └── reports
└── main.tsx
```

---

### 12.3. Componentes principais

- `MapView`
- `RoadLayer`
- `RoadSegmentLayer`
- `ConditionLegend`
- `EvidenceMarker`
- `DashboardCards`
- `KmByConditionChart`
- `EvidenceReviewPanel`
- `SegmentDetailsDrawer`
- `DateFilter`
- `RoadFilter`
- `ConditionFilter`

---

### 12.4. Regras de UI para mapa

- Trechos devem ser renderizados como linhas coloridas.
- Clicar em um trecho deve abrir painel lateral.
- Marcadores de fotos devem aparecer apenas quando ativado o filtro de evidências.
- Trechos sem avaliação devem ser cinza.
- Mapa deve ter legenda fixa.
- Filtro por data deve atualizar cores dos trechos.

---

## 13. Mobile — React Native

### 13.1. Estrutura sugerida

```txt
mobile/src/
├── app
│   ├── navigation.tsx
│   └── providers.tsx
├── shared
│   ├── components
│   ├── hooks
│   ├── services
│   ├── utils
│   └── types
├── features
│   ├── auth
│   ├── inspections
│   ├── camera
│   ├── location
│   ├── sync
│   └── settings
├── database
│   ├── schema.ts
│   ├── migrations.ts
│   └── repositories
└── main.tsx
```

---

### 13.2. Telas mobile

Telas mínimas:

1. Login.
2. Home do agente.
3. Nova vistoria.
4. Câmera/coleta de foto.
5. Lista de fotos da vistoria.
6. Pendências de sincronização.
7. Detalhe da coleta.
8. Configurações.

---

### 13.3. Banco local mobile

Usar SQLite ou alternativa equivalente.

Tabelas locais mínimas:

```txt
local_inspections
local_evidences
sync_queue
local_logs
```

---

### 13.4. Regras de sincronização mobile

1. Cada registro local deve ter `client_uuid`.
2. Cada foto deve ter status local.
3. Upload deve ser feito em fila.
4. Falha não deve apagar registro local.
5. Sucesso deve salvar ID remoto.
6. App deve permitir reenviar.
7. App deve mostrar progresso.
8. App deve suportar fechamento/reabertura sem perder fila.

---

### 13.5. Status local de sincronização

```txt
LOCAL_ONLY
QUEUED
UPLOADING
UPLOADED
SYNCED
ERROR
```

---

### 13.6. Dados capturados por foto

Cada foto deve capturar:

```json
{
  "clientUuid": "uuid",
  "inspectionClientUuid": "uuid",
  "localFileUri": "file://...",
  "latitude": -5.123456,
  "longitude": -39.123456,
  "gpsAccuracyMeters": 12.4,
  "takenAt": "2026-06-21T10:35:00-03:00",
  "note": "Trecho com lama e buracos",
  "syncStatus": "LOCAL_ONLY"
}
```

---

## 14. Modelo de Dados

### 14.1. Entidades principais

```txt
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

### 14.2. Tabela municipalities

```sql
CREATE TABLE municipalities (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    state CHAR(2) NOT NULL,
    ibge_code VARCHAR(20),
    boundary GEOMETRY(MULTIPOLYGON, 4326),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);
```

---

### 14.3. Tabela users

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    municipality_id UUID NULL REFERENCES municipalities(id),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);
```

---

### 14.4. Tabela roads

```sql
CREATE TABLE roads (
    id UUID PRIMARY KEY,
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    name VARCHAR(180) NOT NULL,
    description TEXT,
    geometry GEOMETRY(MULTILINESTRING, 4326),
    total_length_meters NUMERIC(12,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);
```

---

### 14.5. Tabela road_segments

```sql
CREATE TABLE road_segments (
    id UUID PRIMARY KEY,
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    road_id UUID NOT NULL REFERENCES roads(id),
    name VARCHAR(180) NOT NULL,
    segment_order INTEGER,
    geometry GEOMETRY(LINESTRING, 4326) NOT NULL,
    length_meters NUMERIC(12,2) NOT NULL,
    current_condition VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    last_assessment_at TIMESTAMP NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);
```

Índices sugeridos:

```sql
CREATE INDEX idx_road_segments_municipality ON road_segments(municipality_id);
CREATE INDEX idx_road_segments_road ON road_segments(road_id);
CREATE INDEX idx_road_segments_geometry ON road_segments USING GIST(geometry);
CREATE INDEX idx_road_segments_condition ON road_segments(current_condition);
```

---

### 14.6. Tabela inspections

```sql
CREATE TABLE inspections (
    id UUID PRIMARY KEY,
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    field_agent_id UUID NOT NULL REFERENCES users(id),
    client_uuid UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NULL,
    synced_at TIMESTAMP NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(field_agent_id, client_uuid)
);
```

---

### 14.7. Tabela inspection_evidences

```sql
CREATE TABLE inspection_evidences (
    id UUID PRIMARY KEY,
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    inspection_id UUID NOT NULL REFERENCES inspections(id),
    field_agent_id UUID NOT NULL REFERENCES users(id),
    suggested_road_segment_id UUID NULL REFERENCES road_segments(id),
    confirmed_road_segment_id UUID NULL REFERENCES road_segments(id),
    client_uuid UUID NOT NULL,
    file_url TEXT NOT NULL,
    thumbnail_url TEXT NULL,
    storage_key TEXT NOT NULL,
    mime_type VARCHAR(80),
    file_size_bytes BIGINT,
    file_hash VARCHAR(128),
    latitude NUMERIC(10,7) NOT NULL,
    longitude NUMERIC(10,7) NOT NULL,
    location GEOMETRY(POINT, 4326) NOT NULL,
    gps_accuracy_meters NUMERIC(8,2),
    taken_at TIMESTAMP NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP NULL,
    reviewed_by UUID NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    field_note TEXT,
    admin_note TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(field_agent_id, client_uuid)
);
```

Índices:

```sql
CREATE INDEX idx_evidences_municipality ON inspection_evidences(municipality_id);
CREATE INDEX idx_evidences_status ON inspection_evidences(status);
CREATE INDEX idx_evidences_location ON inspection_evidences USING GIST(location);
CREATE INDEX idx_evidences_taken_at ON inspection_evidences(taken_at);
```

---

### 14.8. Tabela road_assessments

```sql
CREATE TABLE road_assessments (
    id UUID PRIMARY KEY,
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    road_segment_id UUID NOT NULL REFERENCES road_segments(id),
    evidence_id UUID NULL REFERENCES inspection_evidences(id),
    condition VARCHAR(30) NOT NULL,
    severity_score INTEGER NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    notes TEXT,
    assessed_by UUID NOT NULL REFERENCES users(id),
    assessed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

Índices:

```sql
CREATE INDEX idx_assessments_segment ON road_assessments(road_segment_id);
CREATE INDEX idx_assessments_municipality ON road_assessments(municipality_id);
CREATE INDEX idx_assessments_assessed_at ON road_assessments(assessed_at);
CREATE INDEX idx_assessments_condition ON road_assessments(condition);
```

---

### 14.9. Tabela occurrences

```sql
CREATE TABLE occurrences (
    id UUID PRIMARY KEY,
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    road_segment_id UUID NOT NULL REFERENCES road_segments(id),
    evidence_id UUID NULL REFERENCES inspection_evidences(id),
    problem_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    severity_score INTEGER NOT NULL,
    description TEXT,
    opened_by UUID NOT NULL REFERENCES users(id),
    opened_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

---

### 14.10. Tabela maintenance_events

```sql
CREATE TABLE maintenance_events (
    id UUID PRIMARY KEY,
    municipality_id UUID NOT NULL REFERENCES municipalities(id),
    road_segment_id UUID NOT NULL REFERENCES road_segments(id),
    occurrence_id UUID NULL REFERENCES occurrences(id),
    type VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    planned_start_date DATE NULL,
    actual_start_date DATE NULL,
    finished_date DATE NULL,
    repaired_length_meters NUMERIC(12,2),
    notes TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

---

### 14.11. Tabela audit_logs

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    municipality_id UUID NULL REFERENCES municipalities(id),
    user_id UUID NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id UUID NULL,
    old_values JSONB NULL,
    new_values JSONB NULL,
    ip_address VARCHAR(80),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL
);
```

---

## 15. APIs REST

### 15.1. Convenções gerais

Prefixo:

```txt
/api/v1
```

Usar JSON.

Paginação padrão:

```txt
?page=0&size=20&sort=createdAt,desc
```

Resposta paginada:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

Erro padrão:

```json
{
  "timestamp": "2026-06-21T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Dados inválidos",
  "details": [
    {
      "field": "name",
      "message": "Nome é obrigatório"
    }
  ]
}
```

---

### 15.2. Auth

```txt
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
```

---

### 15.3. Municípios

```txt
GET    /api/v1/municipalities
POST   /api/v1/municipalities
GET    /api/v1/municipalities/{id}
PUT    /api/v1/municipalities/{id}
DELETE /api/v1/municipalities/{id}
```

---

### 15.4. Usuários

```txt
GET    /api/v1/users
POST   /api/v1/users
GET    /api/v1/users/{id}
PUT    /api/v1/users/{id}
PATCH  /api/v1/users/{id}/activate
PATCH  /api/v1/users/{id}/deactivate
DELETE /api/v1/users/{id}
```

---

### 15.5. Estradas

```txt
GET    /api/v1/roads
POST   /api/v1/roads
GET    /api/v1/roads/{id}
PUT    /api/v1/roads/{id}
DELETE /api/v1/roads/{id}
POST   /api/v1/roads/import-geojson
```

---

### 15.6. Trechos

```txt
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

---

### 15.7. Vistorias

```txt
GET    /api/v1/inspections
POST   /api/v1/inspections
GET    /api/v1/inspections/{id}
PATCH  /api/v1/inspections/{id}/finish
POST   /api/v1/inspections/sync
```

---

### 15.8. Evidências

```txt
GET    /api/v1/evidences
POST   /api/v1/evidences
POST   /api/v1/evidences/upload
GET    /api/v1/evidences/{id}
PATCH  /api/v1/evidences/{id}/approve
PATCH  /api/v1/evidences/{id}/reject
PATCH  /api/v1/evidences/{id}/mark-duplicated
PATCH  /api/v1/evidences/{id}/associate-segment
```

---

### 15.9. Avaliações

```txt
GET    /api/v1/assessments
POST   /api/v1/assessments
GET    /api/v1/assessments/{id}
GET    /api/v1/road-segments/{segmentId}/assessments
```

---

### 15.10. Ocorrências

```txt
GET    /api/v1/occurrences
POST   /api/v1/occurrences
GET    /api/v1/occurrences/{id}
PUT    /api/v1/occurrences/{id}
PATCH  /api/v1/occurrences/{id}/status
```

---

### 15.11. Manutenção

```txt
GET    /api/v1/maintenance-events
POST   /api/v1/maintenance-events
GET    /api/v1/maintenance-events/{id}
PUT    /api/v1/maintenance-events/{id}
PATCH  /api/v1/maintenance-events/{id}/start
PATCH  /api/v1/maintenance-events/{id}/finish
PATCH  /api/v1/maintenance-events/{id}/cancel
```

---

### 15.12. Dashboard

```txt
GET /api/v1/dashboard/summary
GET /api/v1/dashboard/km-by-condition
GET /api/v1/dashboard/map-segments
GET /api/v1/dashboard/maintenance-summary
GET /api/v1/dashboard/occurrences-summary
```

---

### 15.13. Relatórios

```txt
GET  /api/v1/reports/summary.csv
GET  /api/v1/reports/summary.pdf
POST /api/v1/reports/custom
```

---

## 16. Exemplos de Payloads

### 16.1. Criar estrada

```json
{
  "municipalityId": "uuid",
  "name": "Estrada do Sítio Lagoa Nova",
  "description": "Estrada vicinal de acesso à comunidade Lagoa Nova",
  "published": true
}
```

---

### 16.2. Criar trecho

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

Observação: GeoJSON usa ordem `[longitude, latitude]`.

---

### 16.3. Criar avaliação

```json
{
  "roadSegmentId": "uuid",
  "evidenceId": "uuid",
  "condition": "BAD",
  "severityScore": 70,
  "notes": "Trecho com buracos e lama, trânsito lento para veículos pequenos.",
  "assessedAt": "2026-06-21T14:30:00-03:00"
}
```

---

### 16.4. Dashboard summary response

```json
{
  "municipalityId": "uuid",
  "period": {
    "startDate": "2026-06-01",
    "endDate": "2026-06-30"
  },
  "totalMappedKm": 42.75,
  "kmByCondition": {
    "GOOD": 18.4,
    "REGULAR": 9.2,
    "BAD": 10.1,
    "CRITICAL": 3.7,
    "IMPASSABLE": 1.35,
    "UNKNOWN": 0
  },
  "repairedKm": 8.5,
  "openOccurrences": 12,
  "resolvedOccurrences": 5
}
```

---

## 17. Consultas Geográficas Importantes

### 17.1. Calcular comprimento do trecho

Ao salvar uma geometria, calcular comprimento em metros.

Conceito:

```sql
SELECT ST_Length(geometry::geography)
FROM road_segments
WHERE id = :id;
```

---

### 17.2. Encontrar trecho mais próximo de uma foto

Conceito:

```sql
SELECT id
FROM road_segments
WHERE municipality_id = :municipalityId
ORDER BY geometry <-> ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)
LIMIT 1;
```

Para raio máximo:

```sql
SELECT id
FROM road_segments
WHERE municipality_id = :municipalityId
  AND ST_DWithin(
    geometry::geography,
    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
    :radiusMeters
  )
ORDER BY ST_Distance(
    geometry::geography,
    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
)
LIMIT 1;
```

---

### 17.3. Condição histórica por trecho

Conceito:

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

---

## 18. Cálculos de Dashboard

### 18.1. Total mapeado

```txt
total_mapped_km = soma(length_meters dos trechos ativos e publicados) / 1000
```

---

### 18.2. Km por condição atual

```txt
km_GOOD = soma(length_meters de trechos current_condition = GOOD) / 1000
km_REGULAR = soma(length_meters de trechos current_condition = REGULAR) / 1000
km_BAD = soma(length_meters de trechos current_condition = BAD) / 1000
km_CRITICAL = soma(length_meters de trechos current_condition = CRITICAL) / 1000
km_IMPASSABLE = soma(length_meters de trechos current_condition = IMPASSABLE) / 1000
km_UNKNOWN = soma(length_meters de trechos current_condition = UNKNOWN) / 1000
```

---

### 18.3. Km por condição histórica

Para uma data selecionada:

1. buscar última avaliação de cada trecho até a data;
2. assumir `UNKNOWN` se não houver avaliação;
3. somar comprimento por condição.

---

### 18.4. Km recuperados no período

```txt
repaired_km = soma(repaired_length_meters de maintenance_events com status FINISHED e finished_date dentro do período) / 1000
```

---

### 18.5. Percentual por condição

```txt
percentual = km_condicao / total_mapped_km * 100
```

Se `total_mapped_km = 0`, retornar 0 para todos os percentuais.

---

## 19. Critérios de Priorização

No MVP, a priorização pode ser simples.

### 19.1. Score sugerido

```txt
priority_score = severity_score
```

Futuro:

```txt
priority_score = severity_score
               + peso_tipo_problema
               + peso_tempo_sem_manutencao
               + peso_importancia_rota
               + peso_quantidade_evidencias
```

---

### 19.2. Severidade numérica

Sugestão:

| Condição | Severity Score |
|---|---:|
| GOOD | 0 a 20 |
| REGULAR | 21 a 40 |
| BAD | 41 a 70 |
| CRITICAL | 71 a 90 |
| IMPASSABLE | 91 a 100 |

---

## 20. Regras de Validação

### 20.1. Usuário

- Nome obrigatório.
- E-mail obrigatório e único.
- Senha obrigatória na criação.
- Papel obrigatório.
- Município obrigatório exceto para `SUPER_ADMIN`.

---

### 20.2. Município

- Nome obrigatório.
- UF obrigatória com 2 caracteres.
- Código IBGE opcional.

---

### 20.3. Estrada

- Município obrigatório.
- Nome obrigatório.
- Nome não deve duplicar dentro do mesmo município, salvo se permitido explicitamente.

---

### 20.4. Trecho

- Estrada obrigatória.
- Geometria obrigatória.
- Comprimento deve ser maior que zero.
- Condição inicial padrão: `UNKNOWN`.

---

### 20.5. Evidência

- Foto obrigatória.
- Latitude obrigatória.
- Longitude obrigatória.
- Data de captura obrigatória.
- Agente obrigatório.
- Vistoria obrigatória.
- `client_uuid` obrigatório.

---

### 20.6. Avaliação

- Trecho obrigatório.
- Condição obrigatória.
- Severidade obrigatória.
- Avaliador obrigatório.
- Data da avaliação obrigatória.
- Severidade deve estar entre 0 e 100.

---

### 20.7. Intervenção

- Trecho obrigatório.
- Tipo obrigatório.
- Status obrigatório.
- Se status for `FINISHED`, `finished_date` deve ser obrigatório.
- Se informado, `repaired_length_meters` não pode ser negativo.

---

## 21. Segurança e Autorização

### 21.1. JWT

Claims mínimos:

```json
{
  "sub": "user-id",
  "email": "user@email.com",
  "role": "ADMIN_OPERACIONAL",
  "municipalityId": "uuid",
  "iat": 123456789,
  "exp": 123456999
}
```

---

### 21.2. Regra de isolamento por município

Em todo service, validar:

```txt
Se usuário não for SUPER_ADMIN:
    entity.municipality_id deve ser igual ao municipalityId do usuário autenticado.
```

Nunca confiar apenas no filtro do frontend.

---

### 21.3. Upload seguro

Opções:

1. upload direto para backend;
2. upload para storage via URL assinada.

Para MVP, pode ser upload direto para backend.

Para escala, usar URL assinada.

---

## 22. Testes

### 22.1. Testes unitários obrigatórios

Priorizar testes para:

- cálculo de km por condição;
- atualização de condição atual do trecho;
- filtro histórico por data;
- validação multi-tenant;
- idempotência de sincronização;
- associação da evidência ao trecho mais próximo;
- conclusão de intervenção;
- cálculo de km recuperados.

---

### 22.2. Testes de integração

Testar:

- login;
- criação de estrada;
- criação de trecho;
- upload de evidência;
- aprovação de evidência;
- criação de avaliação;
- dashboard summary;
- exportação de relatório.

---

### 22.3. Cenários de aceite

#### Cenário 1 — Coleta e classificação

Dado um agente autenticado no app  
Quando ele tirar uma foto com GPS  
E sincronizar quando tiver internet  
Então a evidência deve aparecer como pendente no admin  
E o admin deve conseguir aprovar e classificar o trecho.

---

#### Cenário 2 — Dashboard da prefeitura

Dado que existem trechos classificados  
Quando o gestor acessar o dashboard  
Então deve visualizar os km por condição  
E o mapa deve exibir as cores correspondentes.

---

#### Cenário 3 — Histórico

Dado que um trecho teve avaliações em janeiro e março  
Quando o usuário selecionar fevereiro  
Então o mapa deve mostrar a condição válida em fevereiro.

---

#### Cenário 4 — Intervenção

Dado que um trecho está crítico  
Quando uma intervenção for concluída com 2 km recuperados  
Então o dashboard do período deve contabilizar 2 km recuperados.

---

## 23. Seed Inicial de Desenvolvimento

Criar seed com:

- 1 município teste;
- 1 super admin;
- 1 admin operacional;
- 1 gestor prefeitura;
- 1 agente de campo;
- 3 estradas;
- 10 trechos;
- avaliações variadas;
- algumas evidências fictícias;
- algumas ocorrências;
- uma intervenção concluída.

---

## 24. Configurações do Sistema

Tabela futura:

```txt
system_settings
municipality_settings
```

Configurações possíveis:

- raio para sugerir trecho próximo;
- precisão mínima recomendada de GPS;
- permitir prefeitura editar classificação;
- publicar avaliações automaticamente;
- cor por condição;
- formatos de relatório habilitados;
- limite máximo de tamanho da foto;
- compressão de imagem no mobile.

Valores iniciais sugeridos:

```txt
near_segment_radius_meters = 100
recommended_gps_accuracy_meters = 30
max_photo_size_mb = 10
image_compression_quality = 0.75
auto_publish_assessments = false
```

---

## 25. Variáveis de Ambiente

### 25.1. Backend

```env
SPRING_PROFILES_ACTIVE=dev
DATABASE_URL=jdbc:postgresql://localhost:5432/sgev
DATABASE_USERNAME=sgev
DATABASE_PASSWORD=sgev
JWT_SECRET=change-me
JWT_ACCESS_EXPIRATION_MINUTES=30
JWT_REFRESH_EXPIRATION_DAYS=7
STORAGE_PROVIDER=minio
STORAGE_BUCKET=sgev-evidences
STORAGE_ENDPOINT=http://localhost:9000
STORAGE_ACCESS_KEY=minioadmin
STORAGE_SECRET_KEY=minioadmin
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

---

### 25.2. Web

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_MAP_PROVIDER=maplibre
```

---

### 25.3. Mobile

```env
API_BASE_URL=http://localhost:8080/api/v1
SYNC_BATCH_SIZE=5
```

---

## 26. Docker Compose Local

Serviços mínimos:

```txt
postgres + postgis
minio
backend
web
```

Opcional:

```txt
redis
nginx
mailhog
```

---

## 27. Definition of Done

Uma funcionalidade só deve ser considerada pronta quando:

- regra de negócio implementada;
- validações implementadas;
- permissões implementadas;
- endpoint documentado;
- testes unitários relevantes criados;
- erro tratado com mensagem clara;
- comportamento multi-tenant validado;
- frontend integrado, quando aplicável;
- logs adequados adicionados;
- cenário de aceite testado manualmente.

---

## 28. Orientações para Agente de IA Desenvolvedor

### 28.1. Prioridade de implementação

Seguir esta ordem:

1. Infra local com Docker Compose.
2. Backend base com autenticação.
3. Modelagem do banco com PostGIS.
4. CRUD de município e usuários.
5. CRUD de estradas.
6. CRUD de trechos com geometria.
7. Cálculo de comprimento dos trechos.
8. Mobile: login e coleta local.
9. Backend: upload de evidências.
10. Admin: revisão de evidências.
11. Avaliação de trecho.
12. Dashboard de km por condição.
13. Mapa colorido.
14. Histórico por data.
15. Ocorrências.
16. Intervenções.
17. Relatórios.

---

### 28.2. Princípios obrigatórios

- Não criar regra de negócio no frontend que deveria estar no backend.
- Não confiar em `municipalityId` vindo do frontend sem validar usuário autenticado.
- Não apagar histórico de avaliações.
- Não salvar imagem diretamente no banco.
- Não usar foto isolada para alterar dashboard sem avaliação aprovada.
- Não criar duplicatas em sincronização mobile.
- Sempre usar `client_uuid` em dados originados do mobile.
- Sempre preservar data de captura original da foto.
- Sempre calcular km com base em trechos.
- Sempre diferenciar status atual de histórico.

---

### 28.3. O que evitar

- Começar pela IA antes do fluxo manual funcionar.
- Criar dashboard sem modelagem de trechos.
- Usar apenas pontos no mapa sem linhas de estrada.
- Deixar prefeitura alterar dados críticos sem auditoria.
- Implementar upload sem idempotência.
- Misturar regras de município no frontend.
- Criar entidades genéricas demais sem domínio claro.

---

## 29. Roadmap Técnico

### Fase 1 — Fundação

- Backend Spring Boot.
- PostgreSQL + PostGIS.
- Autenticação.
- Usuários e municípios.
- Estradas e trechos.
- Mapa web inicial.

### Fase 2 — Coleta

- App mobile.
- Coleta offline.
- Upload de fotos.
- Fila de sincronização.
- Tela admin de evidências.

### Fase 3 — Gestão

- Avaliações.
- Status atual.
- Histórico.
- Ocorrências.
- Intervenções.

### Fase 4 — Dashboard

- Km por condição.
- Mapa colorido.
- Relatórios.
- Filtro temporal.

### Fase 5 — Inteligência

- Sugestão automática de classificação.
- Detecção de problemas por imagem.
- Priorização automática.

---

## 30. Glossário

### Estrada vicinal

Estrada rural, geralmente não pavimentada, que conecta comunidades, sítios, fazendas, escolas, postos de saúde e áreas produtivas ao restante do município.

### Trecho

Parte delimitada de uma estrada, usada como unidade oficial de avaliação e cálculo.

### Evidência

Foto ou registro coletado em campo com localização e data.

### Vistoria

Atividade de coleta realizada por um agente de campo.

### Avaliação

Classificação oficial da condição de um trecho.

### Ocorrência

Problema registrado em um trecho.

### Intervenção

Ação de manutenção ou recuperação realizada pela prefeitura.

### Km recuperados

Quilometragem de estrada melhorada por intervenção concluída.

---

## 31. Resumo Executivo para Desenvolvimento

O sistema deve ser construído em torno de quatro pilares:

1. **Coleta georreferenciada**  
   O agente de campo coleta fotos com GPS, mesmo offline.

2. **Validação administrativa**  
   O admin revisa evidências, associa a trechos e classifica condições.

3. **Mapa de saúde da malha vicinal**  
   A prefeitura visualiza trechos coloridos conforme condição.

4. **Indicadores de gestão**  
   O sistema calcula quilômetros por condição, quilômetros recuperados e histórico de evolução.

A entidade mais importante do sistema é `road_segment`.  
As fotos são evidências.  
As avaliações definem o estado oficial.  
As intervenções comprovam recuperação.  
O dashboard resume tudo para a prefeitura.
