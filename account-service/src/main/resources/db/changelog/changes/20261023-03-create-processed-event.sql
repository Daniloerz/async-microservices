--liquibase formatted sql

--changeset account-service:20261023-03-create-processed-event
CREATE TABLE processed_event (
    event_id     UUID        NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_processed_event PRIMARY KEY (event_id)
);
--rollback DROP TABLE processed_event;
