CREATE TABLE IF NOT EXISTS notifications (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  payment_id BIGINT NOT NULL,
  payment_status VARCHAR(40) NOT NULL,
  channel VARCHAR(40) NOT NULL,
  message VARCHAR(512) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS notification_processed_events (
  id BIGSERIAL PRIMARY KEY,
  event_id VARCHAR(64) NOT NULL UNIQUE,
  processed_at TIMESTAMPTZ NOT NULL
);
