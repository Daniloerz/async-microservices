--liquibase formatted sql

--changeset customer-service:20261022-02-create-outbox-event
CREATE TABLE outbox_event (
    id           BIGINT GENERATED ALWAYS AS IDENTITY,
    event_id     UUID        NOT NULL,
    aggregate_id UUID        NOT NULL,
    event_type   VARCHAR(50) NOT NULL,
    payload      JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    CONSTRAINT pk_outbox_event PRIMARY KEY (id),
    CONSTRAINT uk_outbox_event_event_id UNIQUE (event_id),
    CONSTRAINT ck_outbox_event_event_type CHECK (event_type IN ('CUSTOMER_CREATED', 'CUSTOMER_UPDATED', 'CUSTOMER_DELETED'))
);

-- Only pending events: the relay reads them in id order
CREATE INDEX ix_outbox_event_pending ON outbox_event (id) WHERE published_at IS NULL;
--rollback DROP TABLE outbox_event;
