# Incident AI Platform - Phase 1

Phase 1 delivers the microservice foundation:
- `api-gateway`
- `user-service`
- `order-service`
- `payment-service`

All services are Java 21 + Spring Boot 3, Dockerized, and runnable via Docker Compose.

## Architecture (Phase 1)

- `api-gateway`: single entrypoint, request routing, correlation-id propagation.
- `user-service`: user CRUD APIs backed by PostgreSQL.
- `order-service`: order CRUD APIs backed by PostgreSQL.
- `payment-service`: payment CRUD APIs backed by PostgreSQL.
- Per-service PostgreSQL DB for isolation and service ownership.
- OpenAPI docs on each service for API contract visibility.
- Actuator health endpoints for readiness/liveness checks.

## Run

1. Build jars:
```bash
mvn clean package -DskipTests
```

2. Start stack:
```bash
docker compose up --build
```

3. Verify health:
- Gateway: `http://localhost:8080/actuator/health`
- User: `http://localhost:8081/actuator/health`
- Order: `http://localhost:8082/actuator/health`
- Payment: `http://localhost:8083/actuator/health`

4. OpenAPI:
- User: `http://localhost:8081/swagger-ui/index.html`
- Order: `http://localhost:8082/swagger-ui/index.html`
- Payment: `http://localhost:8083/swagger-ui/index.html`

## Sample API calls (via Gateway)

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}'
```

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"description":"Deploy incident hotfix","amount":250.00}'
```

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"paymentMethod":"CARD","status":"COMPLETED","amount":250.00}'
```

## Environment variables

Each service supports:
- `SERVER_PORT`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

Gateway supports:
- `SERVER_PORT`
- `USER_SERVICE_URL`
- `ORDER_SERVICE_URL`
- `PAYMENT_SERVICE_URL`
