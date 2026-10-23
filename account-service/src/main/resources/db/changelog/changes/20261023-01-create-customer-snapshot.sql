--liquibase formatted sql

--changeset account-service:20261023-01-create-customer-snapshot
CREATE TABLE customer_snapshot (
    customer_id   UUID         NOT NULL,
    name          VARCHAR(100) NOT NULL,
    active        BOOLEAN      NOT NULL,
    last_event_at TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_customer_snapshot PRIMARY KEY (customer_id)
);
--rollback DROP TABLE customer_snapshot;
