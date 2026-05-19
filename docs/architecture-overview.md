# Incident AI Platform: High-Level Design (Phase 1-3)

## 1) System Context (Box View)

```text
+-------------------+        +---------------------------------------------+
|  Client / UI /    |        |                 Platform                    |
|  API Consumer     |------->|  +---------------------+                    |
+-------------------+        |  |     API Gateway     |                    |
                             |  +----------+----------+                    |
                             |             |                               |
                             |   +---------+---------+                     |
                             |   |                   |                     |
                             |   v                   v                     |
                             | +--------+         +--------+               |
                             | | User   |         | Order  |               |
                             | |Service |         |Service |               |
                             | +---+----+         +---+----+               |
                             |     |                  |                    |
                             |     v                  v                    |
                             |  +------+          +--------+               |
                             |  |user_ |          |order_  |               |
                             |  |db    |          |db      |               |
                             |  +------+          +--------+               |
                             |                           |                 |
                             |                           v                 |
                             |                      +---------+            |
                             |                      | Kafka   |            |
                             |                      +----+----+            |
                             |                           |                 |
                             |                           v                 |
                             |                      +---------+            |
                             |                      | Payment |            |
                             |                      | Service |            |
                             |                      +----+----+            |
                             |                           |                 |
                             |                           v                 |
                             |                      +--------+             |
                             |                      |payment_|             |
                             |                      |db      |             |
                             |                      +----+---+             |
                             |                           |                 |
                             |                           v                 |
                             |                     +-------------+         |
                             |                     | Notification|         |
                             |                     | Service     |         |
                             |                     +------+------+\        |
                             |                            |       \        |
                             |                            v        \       |
                             |                        +--------+    \      |
                             |                        |notif_  |     \     |
                             |                        |db      |      \    |
                             |                        +--------+       \   |
                             +------------------------------------------\--+
```

## 2) Business Flow (Request + Event)

```text
+-------------------+      +-------------------+      +-------------------+
| Client            |      | API Gateway       |      | Order Service     |
| POST /api/orders  |----->| Route + Corr-ID   |----->| Save Order (DB)   |
+-------------------+      +-------------------+      +---------+---------+
                                                             |
                                                             | Publish OrderCreated
                                                             v
                                                   +-------------------+
                                                   | Kafka Topic       |
                                                   | incidents.v1.     |
                                                   | order.created     |
                                                   +---------+---------+
                                                             |
                                                             | Consume
                                                             v
                                                   +-------------------+
                                                   | Payment Service   |
                                                   | Idempotency check |
                                                   | Save Payment      |
                                                   +----+----------+---+
                                                        |          |
                     Publish PaymentCompleted/Failed ---+          +--- Publish NotificationTriggered
                                                        |                      |
                                                        v                      v
                                            +-------------------+   +-------------------+
                                            | Kafka Topics      |   | Kafka Topic       |
                                            | payment.*         |   | notification.*    |
                                            +-------------------+   +---------+---------+
                                                                         |
                                                                         | Consume
                                                                         v
                                                              +-----------------------+
                                                              | Notification Service  |
                                                              | Idempotency check     |
                                                              | Save Notification DB  |
                                                              +-----------------------+
```

## 3) Retry and Dead Letter (Order -> Payment)

```text
+-----------------------------------------------+
| Topic: incidents.v1.order.created             |
+---------------------------+-------------------+
                            |
                            v
                  +-----------------------+
                  | payment-service       |
                  | Kafka Consumer        |
                  +-----------+-----------+
                              |
                 +------------+------------+
                 | Success                 | Failure
                 v                         v
      +----------------------+   +----------------------+
      | Process + Publish    |   | Retry #1 (1s)        |
      +----------------------+   +----------+-----------+
                                            |
                                            v
                                 +----------------------+
                                 | Retry #2 (1s)        |
                                 +----------+-----------+
                                            |
                                            v
                                 +----------------------+
                                 | Retry #3 (1s)        |
                                 +----------+-----------+
                                            |
                                            v
                                 +-------------------------------+
                                 | DLT: incidents.v1.order.     |
                                 | created.dlt                   |
                                 +-------------------------------+
```

## 4) Observability Pipeline (Phase 3)

```text
+------------------------+      +------------------------+      +--------------------+
| App Containers         |      | Filebeat               |      | Elasticsearch      |
| api/order/payment/...  |----->| Read Docker logs       |----->| Index logs         |
| JSON stdout logs       |      | Parse + enrich fields  |      | incident-ai-logs-* |
+------------------------+      +-----------+------------+      +---------+----------+
                                             |                             |
                                             |                             v
                                             |                   +--------------------+
                                             +------------------>| Kibana             |
                                                                 | Discover/Dashboard |
                                                                 +--------------------+
```

## 5) Correlation ID Traceability

```text
Client sends header: X-Correlation-Id: obs-xxx
            |
            v
API Gateway filter
  - If missing: generate UUID
  - If present: propagate as-is
            |
            v
Service ingress filters (MDC)
  - user-service
  - order-service
  - payment-service
  - notification-service
            |
            v
Structured logs include: correlationId
            |
            v
Elasticsearch searchable field: correlationId
            |
            v
Kibana query example: correlationId : "obs-xxx"
```

## 6) Deployment View (Local)

```text
docker-compose services:

- api-gateway            :8080
- user-service           :8081
- order-service          :8082
- payment-service        :8083
- notification-service   :8084
- kafka                  :9092
- user-db/order-db/payment-db/notification-db
- elasticsearch          :9200
- kibana                 :5601
- filebeat               (log shipper)
```
