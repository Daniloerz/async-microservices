# Async Microservices — Customers and Accounts

Two Spring Boot microservices that manage bank customers, accounts and movements, and talk to each
other asynchronously through Kafka.

| Service | Responsibility | Port |
|---|---|---|
| `customer-service` | Persons and customers | 8081 |
| `account-service` | Accounts, movements and account statement reports | 8082 |

## Challenge checklist

| Requirement | Where |
|---|---|
| Two microservices: (Customer, Person) and (Account, Movement), communicating asynchronously | `customer-service`, `account-service`, Kafka topic `customer-events` |
| JPA entities; `Customer` inherits from `Person` | `customer-service/.../infrastructure/persistence` (`JOINED` inheritance) |
| Exception handling | `GlobalExceptionHandler` in each service, RFC 9457 Problem Details |
| F1 — CRUD customers, CRU accounts and movements | `/api/v1/customers`, `/api/v1/accounts`, `/api/v1/movements` (GET, POST, PUT, PATCH, DELETE where they apply) |
| F2 — movements update the available balance and are recorded | `POST /api/v1/movements` |
| F3 — "Saldo no disponible" | `422` with `"detail": "Insufficient balance"` |
| F4 — account statement by date range and customer, JSON | `GET /api/v1/reports?customerId=&startDate=&endDate=` |
| F5 — unit test of the `Customer` domain entity | `customer-service/src/test/.../domain/model/CustomerTest.java` |
| F6 — integration tests | `CustomerApiIntegrationTest`, `CustomerEventFlowIntegrationTest`, `MovementApiIntegrationTest`, `ReportApiIntegrationTest` and others (Testcontainers) |
| F7 — deployment in containers | `docker-compose.yml` and a `Dockerfile` per service |
| Database scripts | Liquibase SQL changesets (see [Database scripts](#database-scripts)) |
| Postman collection | [`postman/async-microservices.postman_collection.json`](postman/async-microservices.postman_collection.json) |

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

Compose refuses to start while the database passwords are empty. The first build downloads the Maven
dependencies inside the build image and takes a few minutes.

To stop everything and keep the data, run `docker compose stop`; to also delete all data, run
`docker compose down -v`.

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

### Movements — `account-service` (port 8082)

| Method | Path | Description | Responses |
|---|---|---|---|
| `POST` | `/api/v1/movements` | Register a deposit (positive amount) or a withdrawal (negative amount) | `201` + `Location`, `400`, `422` |
| `GET` | `/api/v1/movements?accountNumber=&page=0&size=20` | List movements from newest to oldest, optionally of one account | `200`, `400` |
| `GET` | `/api/v1/movements/{movementId}` | Get a movement | `200`, `400`, `404` |
| `PUT` | `/api/v1/movements/{movementId}` | Correct the amount of the last movement of its account | `200`, `400`, `404`, `409`, `422` |

```bash
curl -i -X POST http://localhost:8082/api/v1/movements \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"478758","amount":-575}'
```

```json
{
  "movementId": "6b1f2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d",
  "accountNumber": "478758",
  "movementType": "WITHDRAWAL",
  "amount": -575.00,
  "balance": 1425.00,
  "movementDate": "2026-10-23T22:10:00.123456Z"
}
```

- The type (`DEPOSIT` / `WITHDRAWAL`) comes from the sign of `amount`; `amount` cannot be 0.
- `balance` is the account balance right after the movement; the account balance is updated in the
  same transaction.
- A movement that would leave the balance below zero is rejected with `422` and
  `"detail": "Insufficient balance"` (the challenge's "Saldo no disponible"); nothing is saved.
- Inactive or unknown accounts get `422`. Only the last movement of an account can be corrected
  (`409` otherwise); movements are never deleted, so there is no `PATCH` or `DELETE`.
- The date is set by the server (database clock) when the movement is registered.

### Account statement report — `account-service` (port 8082)

| Method | Path | Description | Responses |
|---|---|---|---|
| `GET` | `/api/v1/reports?customerId=&startDate=&endDate=` | Account statement of a customer for a date range (ISO dates, both included) | `200`, `400`, `404` |

```bash
curl "http://localhost:8082/api/v1/reports?customerId=<customerId>&startDate=2026-10-01&endDate=2026-10-31"
```

```json
{
  "customer": { "customerId": "…", "name": "Marianela Montalvo", "active": true },
  "startDate": "2026-10-23",
  "endDate": "2026-10-23",
  "timeZone": "America/Guayaquil",
  "accounts": [
    {
      "accountNumber": "225487",
      "accountType": "CHECKING",
      "active": true,
      "initialBalance": 100.00,
      "openingBalance": 100.00,
      "closingBalance": 700.00,
      "availableBalance": 700.00,
      "totalDeposits": 600.00,
      "totalWithdrawals": 0.00,
      "movements": [
        { "movementId": "…", "movementDate": "2026-10-23T22:10:00.123Z", "movementType": "DEPOSIT",
          "amount": 600.00, "balance": 700.00 }
      ]
    }
  ]
}
```

- The statement lists every account the customer had during the period (also inactive ones and
  accounts without movements) with the balance at the start of the period, at the end of the
  period, the current available balance, the period totals and the movements of the period.
- Days are cut in the configured time zone (`REPORT_TIME_ZONE`, default `America/Guayaquil`);
  periods are limited to `REPORT_MAX_RANGE_DAYS` (default 366) days.
- The whole report is read in one `REPEATABLE READ` read-only transaction, so it is consistent even
  while movements are being registered.

How the challenge's example columns map to the report:

| Challenge column | Report field |
|---|---|
| Fecha | `accounts[].movements[].movementDate` |
| Cliente | `customer.name` |
| Numero Cuenta | `accounts[].accountNumber` |
| Tipo | `accounts[].accountType` |
| Saldo Inicial | `accounts[].initialBalance` |
| Estado | `accounts[].active` |
| Movimiento | `accounts[].movements[].amount` |
| Saldo Disponible | `accounts[].movements[].balance` |

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

## Postman collection

[`postman/async-microservices.postman_collection.json`](postman/async-microservices.postman_collection.json)
(Collection v2.1, works with Postman v9.13.2 and later).

1. Start the system with Docker Compose.
2. Import the file in Postman.
3. Run the whole collection with the **Collection Runner**, in order.

| Folder | Content |
|---|---|
| 0. Health | Both services are up |
| 1. Customers (use case 1) | Jose Lema, Marianela Montalvo and Juan Osorio; get, list, PUT and PATCH |
| 2. Accounts (use cases 2 and 3) | Accounts 478758, 225487, 495878, 496825 and the new checking account 585545 of Jose Lema |
| 3. Movements (use case 4) | Withdrawal 575, deposit 600, deposit 150, withdrawal 540; correction of the last movement |
| 4. Account statement report (use case 5) | Statement of Marianela Montalvo for today |
| 5. Error cases | Insufficient balance, validation errors, conflicts, unknown resources, invalid period |
| 6. Asynchronous flow | Deleting a customer deactivates its accounts through Kafka |

Every request has tests (`pm.test`). Identifications and account numbers get a random suffix per
run (for example `478758` becomes `478758NNNNNN`), so the collection can run many times without
resetting the databases. The base URLs are the collection variables `customerBaseUrl` and
`accountBaseUrl`. A few requests retry for a moment because the customer data reaches
`account-service` asynchronously.

It can also run from the command line with Newman:

```bash
docker run --rm --network host -v "$(pwd)/postman":/etc/newman postman/newman:6-alpine \
  run async-microservices.postman_collection.json
```

(`--network host` works on Linux; elsewhere, install Newman with `npm install -g newman` and run
`newman run postman/async-microservices.postman_collection.json`.)

## Database scripts

Each service owns its database and creates its schema at startup with Liquibase. The scripts are
plain SQL files:

| Database | Scripts |
|---|---|
| `customer_db` | [`customer-service/src/main/resources/db/changelog/changes`](customer-service/src/main/resources/db/changelog/changes) — `person`, `customer`, `outbox_event` |
| `account_db` | [`account-service/src/main/resources/db/changelog/changes`](account-service/src/main/resources/db/changelog/changes) — `customer_snapshot`, `account`, `processed_event`, `movement` |

They are applied in the order of each `db.changelog-master.yaml`. There is no seed data: the
Postman collection creates the challenge data.

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
| `REPORT_TIME_ZONE` | Time zone used to cut report days (account-service) | `America/Guayaquil` |
| `REPORT_MAX_RANGE_DAYS` | Longest report period in days (account-service) | `366` |

Host ports used by Compose can be changed in `.env` (see `.env.example`).

## Tests

Each service is an independent Maven project with its own Maven Wrapper (Java 21 required).
Integration tests use Testcontainers, so Docker must be running.

```bash
cd customer-service && ./mvnw clean test
cd account-service && ./mvnw clean test
```

| Type | What it covers | Examples |
|---|---|---|
| Unit | Domain rules and use cases, with Mockito | `CustomerTest`, `AccountMovementTest`, `RegisterMovementUseCaseTest` |
| Web slice (`@WebMvcTest`) | Validation, status codes and Problem Details | `CustomerControllerTest`, `MovementControllerTest` |
| Integration (Testcontainers: PostgreSQL 17 and Kafka) | Persistence, constraints, outbox, Kafka consumer and dead-letter topic, concurrent withdrawals, report date boundaries | `CustomerApiIntegrationTest`, `CustomerEventFlowIntegrationTest`, `MovementApiIntegrationTest`, `ReportApiIntegrationTest` |

End-to-end validation of the running system is done with the Postman collection.
