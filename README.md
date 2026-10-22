# Async Microservices — Customers and Accounts

Two Spring Boot microservices that manage bank customers, accounts and movements, and talk to each
other asynchronously through Kafka.

| Service | Responsibility | Port |
|---|---|---|
| `customer-service` | Persons and customers | 8081 |
| `account-service` | Accounts, movements and account statement reports | 8082 |

## Architecture

```text
             ┌──────────────────┐   customer events   ┌──────────────────┐
  REST  ───▶ │ customer-service │ ──────(Kafka)─────▶ │ account-service  │ ◀─── REST
             └────────┬─────────┘                     └────────┬─────────┘
                      │                                        │
               PostgreSQL (customer_db)                 PostgreSQL (account_db)
```

- **Database per service:** each service owns its schema; no service reads the other's tables.
- **Asynchronous integration:** `customer-service` publishes customer events; `account-service`
  keeps a local copy of the customer data it needs. No synchronous calls between services.

## Stack

Java 21 · Spring Boot 4.1 · Spring Data JPA (Hibernate 7) · Liquibase · PostgreSQL 17 ·
Apache Kafka 4.2 (KRaft) · MapStruct · Lombok · springdoc-openapi · JUnit 5, Mockito,
Testcontainers · Docker Compose.

## Run with Docker

Requirements: Docker with Docker Compose v2.

```bash
cp .env.example .env
# edit .env and set CUSTOMER_DB_PASSWORD and ACCOUNT_DB_PASSWORD
docker compose up --build -d
docker compose ps
```

Compose refuses to start while the database passwords are empty.

Health checks:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## Configuration

Each service reads its configuration from environment variables (defaults point to the ports
published by Docker Compose, so a service can also run from the IDE against the Compose
infrastructure).

| Variable | Description | Default |
|---|---|---|
| `SERVER_PORT` | HTTP port | `8081` / `8082` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | PostgreSQL location | `localhost`, `5434` / `5435`, `customer_db` / `account_db` |
| `DB_USERNAME` | Database user | `customer_user` / `account_user` |
| `DB_PASSWORD` | Database password | none (required) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9094` |

Host ports used by Compose can be changed in `.env` (see `.env.example`).

## Tests

Each service is an independent Maven project. Integration tests use Testcontainers, so Docker must
be running.

```bash
cd customer-service && ./mvnw clean test
cd account-service && ./mvnw clean test
```
