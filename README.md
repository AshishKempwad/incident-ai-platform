# Incident AI Platform

## Phase 1 + Phase 2 Status

This repository now contains:
- `api-gateway`
- `user-service`
- `order-service`
- `payment-service`
- `notification-service` (added in Phase 2)

Phase 3 observability guide:
- [`docs/phase-3-observability.md`](/Users/ashishkempwad/ProjectG/incident-ai-platform/docs/phase-3-observability.md)

Phase 2 introduces Kafka-based event-driven flow:

`order-service` -> `Kafka` -> `payment-service` -> `Kafka` -> `notification-service`

## Why this architecture exists

- `order-service` remains the source of truth for order creation, and emits `OrderCreated`.
- `payment-service` asynchronously reacts to order events, executes payment outcome logic, and emits downstream payment + notification events.
- `notification-service` consumes notification trigger events and persists notification records.
- Kafka decouples service availability and processing speed from request latency.
- DLQ + retries + idempotency are in place to make event processing resilient and replay-safe.

## Topic naming strategy

Topics use: `<domain>.v<version>.<entity>.<action>`

- `incidents.v1.order.created`
- `incidents.v1.payment.completed`
- `incidents.v1.payment.failed`
- `incidents.v1.notification.triggered`

DLQ topics use `<topic>.dlt`:
- `incidents.v1.order.created.dlt`
- `incidents.v1.notification.triggered.dlt`

## Event contracts (JSON schemas)

Schemas are versioned under [`contracts/events`](/Users/ashishkempwad/ProjectG/incident-ai-platform/contracts/events):
- [`order-created.v1.schema.json`](/Users/ashishkempwad/ProjectG/incident-ai-platform/contracts/events/order-created.v1.schema.json)
- [`payment-completed.v1.schema.json`](/Users/ashishkempwad/ProjectG/incident-ai-platform/contracts/events/payment-completed.v1.schema.json)
- [`payment-failed.v1.schema.json`](/Users/ashishkempwad/ProjectG/incident-ai-platform/contracts/events/payment-failed.v1.schema.json)
- [`notification-triggered.v1.schema.json`](/Users/ashishkempwad/ProjectG/incident-ai-platform/contracts/events/notification-triggered.v1.schema.json)

Each event contains:
- envelope metadata (`eventId`, `eventType`, `eventVersion`, `occurredAt`, `source`, `correlationId`)
- typed `payload`
- Kafka header `event-version=v1`

## Retry, DLQ, and idempotency

- `payment-service` consumer retries `OrderCreated` 3 times with 1s backoff, then publishes to `incidents.v1.order.created.dlt`.
- `notification-service` consumer retries `NotificationTriggered` 3 times with 1s backoff, then publishes to `incidents.v1.notification.triggered.dlt`.
- Idempotency is enforced by persisted processed-event tables:
  - `payment_processed_events`
  - `notification_processed_events`
- Duplicate `eventId` is skipped safely.

## Build

```bash
mvn clean package -DskipTests
```

## Local run (Docker)

```bash
docker compose up --build
```

Services:
- Gateway: `http://localhost:8080`
- User: `http://localhost:8081`
- Order: `http://localhost:8082`
- Payment: `http://localhost:8083`
- Notification: `http://localhost:8084`
- Kafka broker: `localhost:9092`

PostgreSQL:
- user-db `localhost:5433`
- order-db `localhost:5434`
- payment-db `localhost:5435`
- notification-db `localhost:5436`

## Local test flow (Phase 2)

1. Create order through gateway:
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: test-corr-001" \
  -d '{"userId":1,"description":"Deploy hotfix","amount":250.00}'
```

2. Check payment records:
```bash
curl http://localhost:8080/api/payments
```

3. Check notification records:
```bash
curl http://localhost:8080/api/notifications
```

Expected:
- An `OrderCreated` event is produced.
- `payment-service` consumes it, persists payment, and emits:
  - `PaymentCompleted` or `PaymentFailed`
  - `NotificationTriggered`
- `notification-service` consumes notification event and persists notification row.

## Configuration variables

For Kafka-enabled services:
- `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`)
- `KAFKA_GROUP_ID` (payment/notification consumers)

For service DB:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

For ports:
- `SERVER_PORT`

## Notes

- Existing REST APIs from Phase 1 remain available.
- Phase 2 intentionally stops at synchronous REST + asynchronous event backbone.
- No further phases are implemented here.
