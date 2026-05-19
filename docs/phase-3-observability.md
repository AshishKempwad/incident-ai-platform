# Phase 3: Centralized Logging and Observability

## Scope Delivered

This phase adds centralized observability for the platform:

- Structured JSON logs for all services
- Correlation ID propagation and indexing
- Elasticsearch for log indexing and search
- Kibana for exploration and dashboards
- Filebeat pipeline for log shipping from Docker containers

## Architecture

Flow:

`Spring services -> Docker container stdout (JSON) -> Filebeat -> Elasticsearch index -> Kibana dashboards`

## Implemented Components

### 1) Elasticsearch setup

Defined in [`docker-compose.yml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/docker-compose.yml):

- Service: `elasticsearch`
- Image: `docker.elastic.co/elasticsearch/elasticsearch:8.14.3`
- Single-node mode for local/dev:
  - `discovery.type=single-node`
  - `xpack.security.enabled=false`
- Port: `9200`
- Persistent volume: `es-data`

### 2) Kibana setup

Defined in [`docker-compose.yml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/docker-compose.yml):

- Service: `kibana`
- Image: `docker.elastic.co/kibana/kibana:8.14.3`
- Connected to Elasticsearch via:
  - `ELASTICSEARCH_HOSTS=http://elasticsearch:9200`
- Port: `5601`

### 3) Filebeat setup

Defined in:
- Compose service: [`docker-compose.yml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/docker-compose.yml)
- Config file: [`observability/filebeat/filebeat.yml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/observability/filebeat/filebeat.yml)

Pipeline details:

- Reads Docker JSON log files from `/var/lib/docker/containers/*/*.log`
- Parses container envelope
- Drops non-JSON lines early
- Decodes JSON payload from service stdout
- Drops Filebeat self-logs from the shipping stream
- Sends to Elasticsearch index:
  - `incident-ai-logs-YYYY.MM.DD`

## Structured Logging Configuration

All service logs are now structured JSON through logback encoder.

Dependency added to service modules:
- `net.logstash.logback:logstash-logback-encoder:7.4`

Configured files:
- [`api-gateway/src/main/resources/logback-spring.xml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/api-gateway/src/main/resources/logback-spring.xml)
- [`user-service/src/main/resources/logback-spring.xml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/user-service/src/main/resources/logback-spring.xml)
- [`order-service/src/main/resources/logback-spring.xml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/order-service/src/main/resources/logback-spring.xml)
- [`payment-service/src/main/resources/logback-spring.xml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/payment-service/src/main/resources/logback-spring.xml)
- [`notification-service/src/main/resources/logback-spring.xml`](/Users/ashishkempwad/ProjectG/incident-ai-platform/notification-service/src/main/resources/logback-spring.xml)

Standardized fields:
- `@timestamp`
- `service_name`
- `level`
- `logger`
- `thread`
- `message`
- `correlationId` (for servlet services via MDC)
- `stack_trace` (for exception logs)

## Correlation ID Path

Implemented filters:
- Gateway: [`api-gateway/src/main/java/com/platform/gateway/filter/CorrelationIdFilter.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/api-gateway/src/main/java/com/platform/gateway/filter/CorrelationIdFilter.java)
- User: [`user-service/src/main/java/com/platform/user/config/CorrelationIdFilter.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/user-service/src/main/java/com/platform/user/config/CorrelationIdFilter.java)
- Order: [`order-service/src/main/java/com/platform/order/config/CorrelationIdFilter.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/order-service/src/main/java/com/platform/order/config/CorrelationIdFilter.java)
- Payment: [`payment-service/src/main/java/com/platform/payment/config/CorrelationIdFilter.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/payment-service/src/main/java/com/platform/payment/config/CorrelationIdFilter.java)
- Notification: [`notification-service/src/main/java/com/platform/notification/config/CorrelationIdFilter.java`](/Users/ashishkempwad/ProjectG/incident-ai-platform/notification-service/src/main/java/com/platform/notification/config/CorrelationIdFilter.java)

Tracing pattern:
1. Client sets `X-Correlation-Id`
2. Gateway forwards it
3. Services store it in MDC
4. JSON logs emit `correlationId`
5. Kibana can trace request hop-by-hop

## Run (Phase 3)

```bash
mvn clean package -DskipTests
docker compose up --build
```

Open:
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`

## Kibana Setup (Index Pattern)

Create data view:
- Name: `incident-ai-logs-*`
- Time field: `@timestamp`

## Sample Kibana Queries (KQL)

By service:
```text
service_name : "payment-service"
```

Error logs:
```text
level : "ERROR"
```

Correlation trace:
```text
correlationId : "test-corr-003"
```

Order lifecycle events:
```text
message : "*order_created*" or message : "*order_created_received*"
```

Payment failures:
```text
message : "*payment_failed*"
```

Notification path:
```text
message : "*notification_triggered*" or message : "*notification_persisted*"
```

## Elasticsearch Search API Examples

Search last 100 errors:
```bash
curl -s "http://localhost:9200/incident-ai-logs-*/_search" -H "Content-Type: application/json" -d '{
  "size": 100,
  "sort": [{"@timestamp": "desc"}],
  "query": {"term": {"level.keyword": "ERROR"}}
}'
```

Search by correlation ID:
```bash
curl -s "http://localhost:9200/incident-ai-logs-*/_search" -H "Content-Type: application/json" -d '{
  "sort": [{"@timestamp": "asc"}],
  "query": {"term": {"correlationId.keyword": "test-corr-003"}}
}'
```

Event frequency by service (aggregation):
```bash
curl -s "http://localhost:9200/incident-ai-logs-*/_search" -H "Content-Type: application/json" -d '{
  "size": 0,
  "aggs": {
    "services": {
      "terms": {"field": "service_name.keyword", "size": 20}
    }
  }
}'
```

## Suggested Error Dashboard Panels

Use Kibana Lens/Visualize with data view `incident-ai-logs-*`.

Panel 1: Error count over time
- Metric: Count
- Filter: `level:ERROR`
- Breakdown: `service_name.keyword`

Panel 2: Top failing loggers
- Terms aggregation on `logger.keyword`
- Filter: `level:ERROR`

Panel 3: Dead letter indicators
- Filter:
  - `message:*dlt* OR message:*dead letter* OR message:*DefaultErrorHandler*`

Panel 4: Correlation trace table
- Columns:
  - `@timestamp`, `service_name`, `level`, `correlationId`, `message`

Panel 5: Payment pipeline health
- Filter:
  - `service_name:"payment-service"`
  - `message:*order_created_received* OR message:*payment_processed* OR message:*payment_failed*`

## Production Hardening Notes

Current setup is production-pattern aligned, but local-grade defaults are used for speed:
- Elasticsearch security disabled (`xpack.security.enabled=false`) for local
- Single-node cluster
- No ILM/rollover policy yet
- No alerting rules yet

Recommended next step in production:
- Enable TLS/auth
- Add index lifecycle management
- Add dashboard export/import (`ndjson`) under version control
- Add alerting on error-rate, DLT growth, and consumer lag
