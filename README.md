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

## API

Interactive documentation (Swagger UI) is available on each service at `/swagger-ui.html`, and the
OpenAPI document at `/v3/api-docs`. Errors use RFC 9457 Problem Details (`application/problem+json`);
validation errors add an `errors` array of `{field, message}`.

### Customers — `customer-service` (port 8081)

| Method | Path | Description | Responses |
|---|---|---|---|
| `POST` | `/api/v1/customers` | Create a customer | `201` + `Location`, `400`, `409` |
| `GET` | `/api/v1/customers?page=0&size=20` | List customers ordered by name (`size` ≤ 100) | `200`, `400` |
| `GET` | `/api/v1/customers/{customerId}` | Get a customer | `200`, `400`, `404` |
| `PUT` | `/api/v1/customers/{customerId}` | Replace the customer data (`password` optional) | `200`, `400`, `404`, `409` |
| `PATCH` | `/api/v1/customers/{customerId}` | Change only the fields sent | `200`, `400`, `404`, `409` |
| `DELETE` | `/api/v1/customers/{customerId}` | Delete a customer | `204`, `404`, `409` |

```bash
curl -i -X POST http://localhost:8081/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Jose Lema","gender":"MALE","age":35,"identification":"1712345678",
       "address":"Otavalo sn y principal","phone":"098254785","password":"1234"}'
```

- `customerId` is a UUID that the service assigns; the numeric database key is never exposed.
- `gender`: `MALE`, `FEMALE` or `OTHER`; `age`: 18–120; `identification` is unique.
- The password is stored as a BCrypt hash and is never returned.
- `409` is returned for a duplicated identification and when another request changed the same
  customer at the same time (optimistic locking).

### Accounts — `account-service` (port 8082)

| Method | Path | Description | Responses |
|---|---|---|---|
| `POST` | `/api/v1/accounts` | Open an account for an active customer | `201` + `Location`, `400`, `409`, `422` |
| `GET` | `/api/v1/accounts?customerId=&page=0&size=20` | List accounts ordered by number, optionally of one customer | `200`, `400` |
| `GET` | `/api/v1/accounts/{accountNumber}` | Get an account | `200`, `400`, `404` |
| `PUT` | `/api/v1/accounts/{accountNumber}` | Replace the account type and status | `200`, `400`, `404`, `422` |
| `PATCH` | `/api/v1/accounts/{accountNumber}` | Change the account type or status | `200`, `400`, `404`, `422` |

```bash
curl -i -X POST http://localhost:8082/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"478758","accountType":"SAVINGS","initialBalance":2000,
       "customerId":"<customerId returned by customer-service>"}'
```

- `accountNumber` (6–20 digits) is chosen by the client and identifies the account in the URL.
- `accountType`: `SAVINGS` or `CHECKING`. `initialBalance` ≥ 0 with up to 2 decimals; `balance`
  starts equal to it.
- Account number, customer and initial balance cannot change after opening.
- `422` means the customer is unknown to `account-service`, not active, or an account of an inactive
  customer was being activated.
- Accounts are not deleted (the challenge asks for create, read and update).

## Events

`customer-service` publishes every change on the `customer-events` topic (3 partitions). The key is
the `customerId`, so all events of one customer keep their order.

```json
{
  "eventId": "8a3a3f8e-5c2b-4a8e-9d6f-1b2c3d4e5f60",
  "eventType": "CUSTOMER_CREATED",
  "version": 1,
  "occurredAt": "2026-10-22T21:00:00Z",
  "customer": { "customerId": "3f6c2a0e-8f1d-4c55-9a51-0d1f3e7b9a10", "name": "Jose Lema", "active": true }
}
```

`eventType` is `CUSTOMER_CREATED`, `CUSTOMER_UPDATED` or `CUSTOMER_DELETED`. Events carry only the
data other services need: no identification, contact data or password.

`account-service` consumes them (consumer group `account-service`):

| Event | Effect in `account-service` |
|---|---|
| `CUSTOMER_CREATED` / `CUSTOMER_UPDATED` | Creates or replaces the local copy of the customer (`customer_snapshot`) |
| `CUSTOMER_DELETED` | Marks the customer inactive and deactivates all its accounts |

- A repeated event (same `eventId`) is ignored; an event older than the last one applied is ignored.
- Invalid messages go straight to `customer-events.DLT`; other failures are retried 3 times (1 s
  apart) and then sent to the same dead-letter topic.

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic customer-events.DLT --from-beginning --property print.key=true
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
| `CUSTOMER_EVENTS_TOPIC` | Topic for customer events (both services) | `customer-events` |
| `OUTBOX_RELAY_DELAY_MS` | Pause between outbox relay runs (customer-service) | `1000` |
| `OUTBOX_BATCH_SIZE` | Maximum events sent per relay run (customer-service) | `100` |
| `OUTBOX_SEND_TIMEOUT` | Maximum wait for the broker per event (customer-service) | `10s` |
| `KAFKA_CONSUMER_GROUP` | Consumer group (account-service) | `account-service` |
| `CUSTOMER_EVENTS_DLT` | Dead-letter topic (account-service) | `customer-events.DLT` |
| `CUSTOMER_EVENTS_RETRY_ATTEMPTS` | Retries before sending a message to the DLT (account-service) | `3` |
| `CUSTOMER_EVENTS_RETRY_INTERVAL` | Pause between retries (account-service) | `1s` |

Host ports used by Compose can be changed in `.env` (see `.env.example`).

## Tests

Each service is an independent Maven project. Integration tests use Testcontainers, so Docker must
be running.

```bash
cd customer-service && ./mvnw clean test
cd account-service && ./mvnw clean test
```
