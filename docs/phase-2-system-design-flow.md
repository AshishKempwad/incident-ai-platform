# Incident AI Platform: Phase 2 Design Flow (File/Class/Method Level)

## 1. Purpose

This document explains how the current codebase works end-to-end for new joiners, with concrete traceability to files, classes, and methods.

Scope:
- Phase 1 synchronous REST foundation
- Phase 2 Kafka event-driven flow
- Retry/DLQ/idempotency behavior

---

## 2. Service Topology

Services in scope:
- `api-gateway` (entrypoint + routing)
- `order-service` (order write + event producer)
- `payment-service` (order event consumer + payment processing + downstream producers)
- `notification-service` (notification event consumer + persistence)
- `user-service` (still part of Phase 1 CRUD foundation)

Infra in local setup:
- Kafka broker (KRaft mode) via [`docker-compose.yml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/docker-compose.yml)
- PostgreSQL DB per service via [`db/init/*.sql`](/Users/ashishkempwad/ProjectG/incident-ai-platform/db/init)

---

## 3. Topic Strategy and Contracts

Topic naming convention:
- `<domain>.v<version>.<entity>.<action>`

Current topics:
- `incidents.v1.order.created`
- `incidents.v1.payment.completed`
- `incidents.v1.payment.failed`
- `incidents.v1.notification.triggered`

Dead-letter topics:
- `incidents.v1.order.created.dlt`
- `incidents.v1.notification.triggered.dlt`

Contract schemas (JSON Schema):
- [`contracts/events/order-created.v1.schema.json`](/Users/ashishkempwad/ProjectG/incident-ai-platform/contracts/events/order-created.v1.schema.json)
- [`contracts/events/payment-completed.v1.schema.json`](/Users/ashishkempwad/ProjectG/incident-ai-platform/contracts/events/payment-completed.v1.schema.json)
- [`contracts/events/payment-failed.v1.schema.json`](/Users/ashishkempwad/ProjectG/incident-ai-platform/contracts/events/payment-failed.v1.schema.json)
- [`contracts/events/notification-triggered.v1.schema.json`](/Users/ashishkempwad/ProjectG/incident-ai-platform/contracts/events/notification-triggered.v1.schema.json)

Versioning:
- Envelope field: `eventVersion = "v1"`
- Kafka header: `event-version = "v1"`

---

## 4. Synchronous Request Path (Create Order)

### 4.1 Gateway routing

File: [`api-gateway/src/main/resources/application.yml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/api-gateway/src/main/resources/application.yml)
- Route `Path=/api/orders/**` -> `${ORDER_SERVICE_URL}`

File: [`api-gateway/src/main/java/com/platform/gateway/filter/CorrelationIdFilter.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/api-gateway/src/main/java/com/platform/gateway/filter/CorrelationIdFilter.java)
- Class: `CorrelationIdFilter`
- Method: `filter(...)`
  - Reads `X-Correlation-Id` header or generates UUID
  - Propagates correlation id downstream

### 4.2 Order controller and service

File: [`order-service/src/main/java/com/platform/order/controller/OrderController.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/order-service/src/main/java/com/platform/order/controller/OrderController.java)
- Class: `OrderController`
- Method: `create(@Valid @RequestBody OrderRequestDto dto)`
  - Entrypoint for `POST /api/orders`
  - Delegates to `OrderService#create`

File: [`order-service/src/main/java/com/platform/order/service/OrderService.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/order-service/src/main/java/com/platform/order/service/OrderService.java)
- Class: `OrderService`
- Method: `create(OrderRequestDto dto)` (`@Transactional`)
  1. Maps DTO -> entity via `OrderMapper`
  2. Persists order via `OrderRepository#save`
  3. Calls `OrderEventProducer#publishOrderCreated`
  4. Returns `OrderResponseDto`

File: [`order-service/src/main/java/com/platform/order/repository/OrderRepository.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/order-service/src/main/java/com/platform/order/repository/OrderRepository.java)
- JPA repository for `OrderEntity`

File: [`db/init/order-init.sql`](/Users/ashishkempwad/ProjectG/incident-ai-platform/db/init/order-init.sql)
- Owns `orders` table schema

---

## 5. Asynchronous Event Path

## 5.1 Step A: OrderCreated production

File: [`order-service/src/main/java/com/platform/order/producer/OrderEventProducer.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/order-service/src/main/java/com/platform/order/producer/OrderEventProducer.java)
- Class: `OrderEventProducer`
- Method: `publishOrderCreated(OrderEntity orderEntity)`
  - Builds `OrderCreatedEvent` envelope
  - Sets key = `orderId`
  - Sets header `event-version=v1`
  - Publishes to topic `incidents.v1.order.created`

File: [`order-service/src/main/java/com/platform/order/events/OrderCreatedEvent.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/order-service/src/main/java/com/platform/order/events/OrderCreatedEvent.java)
- Event envelope model

File: [`order-service/src/main/java/com/platform/order/events/OrderCreatedPayload.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/order-service/src/main/java/com/platform/order/events/OrderCreatedPayload.java)
- Domain payload model

File: [`order-service/src/main/java/com/platform/order/config/KafkaTopicConfig.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/order-service/src/main/java/com/platform/order/config/KafkaTopicConfig.java)
- Creates main topics at startup (`NewTopic` beans)

## 5.2 Step B: Payment service consumes OrderCreated

File: [`payment-service/src/main/java/com/platform/payment/consumer/OrderEventConsumer.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/payment-service/src/main/java/com/platform/payment/consumer/OrderEventConsumer.java)
- Class: `OrderEventConsumer`
- Method: `consume(OrderCreatedEvent event, @Header("event-version") String version)`
  - Listener on `incidents.v1.order.created`
  - Delegates to `PaymentEventService#process`

File: [`payment-service/src/main/java/com/platform/payment/service/PaymentEventService.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/payment-service/src/main/java/com/platform/payment/service/PaymentEventService.java)
- Class: `PaymentEventService`
- Method: `process(OrderCreatedEvent event)` (`@Transactional`)
  1. Idempotency check: `ProcessedEventRepository#existsByEventId`
  2. If duplicate: log + return
  3. Create payment row (`PaymentRepository#save`)
  4. Save processed event id (`ProcessedEventRepository#save`)
  5. Publish either:
     - `publishPaymentCompleted(...)` OR
     - `publishPaymentFailed(...)`
  6. Publish notification trigger: `publishNotificationTriggered(...)`

Current payment outcome logic:
- `amount <= 1000` -> `COMPLETED`
- else -> `FAILED` (`reason=amount_limit_exceeded`)

File: [`payment-service/src/main/java/com/platform/payment/entity/ProcessedEventEntity.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/payment-service/src/main/java/com/platform/payment/entity/ProcessedEventEntity.java)
- Persists dedup key (`eventId`)

File: [`db/init/payment-init.sql`](/Users/ashishkempwad/ProjectG/incident-ai-platform/db/init/payment-init.sql)
- Owns:
  - `payments`
  - `payment_processed_events`

## 5.3 Step C: Payment service publishes downstream events

File: [`payment-service/src/main/java/com/platform/payment/producer/PaymentEventProducer.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/payment-service/src/main/java/com/platform/payment/producer/PaymentEventProducer.java)
- Class: `PaymentEventProducer`
- Method: `publishPaymentCompleted(...)`
  - Produces `PaymentCompletedEvent` -> `incidents.v1.payment.completed`
- Method: `publishPaymentFailed(...)`
  - Produces `PaymentFailedEvent` -> `incidents.v1.payment.failed`
- Method: `publishNotificationTriggered(...)`
  - Produces `NotificationTriggeredEvent` -> `incidents.v1.notification.triggered`
- Method: `send(...)`
  - Common Kafka send path with topic/key/version header

Event model files:
- [`payment-service/src/main/java/com/platform/payment/events/PaymentCompletedEvent.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/payment-service/src/main/java/com/platform/payment/events/PaymentCompletedEvent.java)
- [`payment-service/src/main/java/com/platform/payment/events/PaymentFailedEvent.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/payment-service/src/main/java/com/platform/payment/events/PaymentFailedEvent.java)
- [`payment-service/src/main/java/com/platform/payment/events/NotificationTriggeredEvent.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/payment-service/src/main/java/com/platform/payment/events/NotificationTriggeredEvent.java)

## 5.4 Step D: Notification service consumes NotificationTriggered

File: [`notification-service/src/main/java/com/platform/notification/consumer/NotificationEventConsumer.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/notification-service/src/main/java/com/platform/notification/consumer/NotificationEventConsumer.java)
- Class: `NotificationEventConsumer`
- Method: `consume(NotificationTriggeredEvent event, @Header("event-version") String version)`
  - Listener on `incidents.v1.notification.triggered`
  - Delegates to `NotificationEventService#process`

File: [`notification-service/src/main/java/com/platform/notification/service/NotificationEventService.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/notification-service/src/main/java/com/platform/notification/service/NotificationEventService.java)
- Class: `NotificationEventService`
- Method: `process(NotificationTriggeredEvent event)` (`@Transactional`)
  1. Idempotency check by eventId
  2. Persist `notifications` row
  3. Persist processed-event marker

File: [`db/init/notification-init.sql`](/Users/ashishkempwad/ProjectG/incident-ai-platform/db/init/notification-init.sql)
- Owns:
  - `notifications`
  - `notification_processed_events`

---

## 6. Retry and Dead Letter Flow

## 6.1 Payment consumer retry/DLT

File: [`payment-service/src/main/java/com/platform/payment/config/KafkaConfig.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/payment-service/src/main/java/com/platform/payment/config/KafkaConfig.java)
- Bean: `orderCreatedErrorHandler(...)`
  - `DefaultErrorHandler`
  - Backoff: 1 second
  - Retries: 3 attempts
  - On exhaustion -> publish record to `incidents.v1.order.created.dlt`

## 6.2 Notification consumer retry/DLT

File: [`notification-service/src/main/java/com/platform/notification/config/KafkaConfig.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/notification-service/src/main/java/com/platform/notification/config/KafkaConfig.java)
- Bean: `notificationConsumerErrorHandler(...)`
  - `DefaultErrorHandler`
  - Backoff: 1 second
  - Retries: 3 attempts
  - On exhaustion -> publish to `incidents.v1.notification.triggered.dlt`

---

## 7. Class Ownership by Responsibility

### Routing and cross-cutting
- Gateway route config: `api-gateway/.../application.yml`
- Correlation id (gateway): `gateway/filter/CorrelationIdFilter`
- Correlation id (service ingress): `*/config/CorrelationIdFilter`

### Domain write side
- Order write API: `order/controller/OrderController`
- Order orchestration: `order/service/OrderService`
- Payment event processing: `payment/service/PaymentEventService`
- Notification event processing: `notification/service/NotificationEventService`

### Messaging
- Topic constants:
  - `order/config/TopicNames`
  - `payment/config/TopicNames`
  - `notification/config/TopicNames`
- Producers:
  - `order/producer/OrderEventProducer`
  - `payment/producer/PaymentEventProducer`
- Consumers:
  - `payment/consumer/OrderEventConsumer`
  - `notification/consumer/NotificationEventConsumer`

### Persistence
- `OrderRepository`, `PaymentRepository`, `NotificationRepository`
- Idempotency repos:
  - `ProcessedEventRepository` (payment)
  - `ProcessedEventRepository` (notification)

---

## 8. Runtime Sequence (Single Order)

1. `POST /api/orders` hits `OrderController#create`.
2. `OrderService#create` persists order and calls `OrderEventProducer#publishOrderCreated`.
3. Kafka receives `OrderCreated` on `incidents.v1.order.created`.
4. `OrderEventConsumer#consume` in payment service receives event.
5. `PaymentEventService#process` dedups, persists payment, emits:
   - `PaymentCompleted` or `PaymentFailed`
   - `NotificationTriggered`
6. `NotificationEventConsumer#consume` receives `NotificationTriggered`.
7. `NotificationEventService#process` dedups and persists notification.
8. Client can fetch via:
   - `GET /api/payments`
   - `GET /api/notifications`

---

## 9. Configuration Hotspots

Core config files:
- [`docker-compose.yml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/docker-compose.yml)
- [`order-service/src/main/resources/application.yml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/order-service/src/main/resources/application.yml)
- [`payment-service/src/main/resources/application.yml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/payment-service/src/main/resources/application.yml)
- [`notification-service/src/main/resources/application.yml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/notification-service/src/main/resources/application.yml)

Key env vars:
- `KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_GROUP_ID` (consumer services)
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `SERVER_PORT`

---

## 10. Known Current Boundaries (for next phases)

- Event publishing is not yet transactional-outbox based.
- No schema registry integration yet.
- No dedicated consumer for `payment.completed` / `payment.failed` topics yet.
- No integration tests for Kafka flow yet.

These are expected next hardening steps beyond current Phase 2 scope.
