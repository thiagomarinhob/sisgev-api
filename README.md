# sisgev-api

Backend do **SGEV — Sistema de Gestão de Estradas Vicinais**.
Java 21 · Spring Boot · PostgreSQL + PostGIS · MinIO.

> Regras de negócio e specs em [`docs/`](./docs).

## Pré-requisitos

- Java 21+ e Maven 3.9+
- Docker + Docker Compose

## Subir o ambiente local (Postgres+PostGIS + MinIO)

```bash
docker compose up -d
```

Serviços:

| Serviço | Porta | Acesso |
|---|---|---|
| PostgreSQL + PostGIS | `5432` | `sgev` / `sgev` (db `sgev`) |
| MinIO (API S3) | `9000` | `minioadmin` / `minioadmin` |
| MinIO (Console) | `9001` | http://localhost:9001 |

O bucket `sgev-evidences` é criado automaticamente.

## Rodar o backend

O projeto Spring Boot está na raiz (Maven Wrapper incluído):

```bash
./mvnw spring-boot:run
```

- API em `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- O **Flyway** aplica as migrations de `src/main/resources/db/migration` no startup.

## Configuração (variáveis de ambiente)

Defaults já apontam para o `docker-compose`. Para sobrescrever:

```
DATABASE_URL=jdbc:postgresql://localhost:5432/sgev
DATABASE_USERNAME=sgev
DATABASE_PASSWORD=sgev
```

## Parar o ambiente

```bash
docker compose down        # mantém os dados (volumes)
docker compose down -v     # apaga os dados também
```
