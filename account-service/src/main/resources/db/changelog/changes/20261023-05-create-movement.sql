--liquibase formatted sql

--changeset account-service:20261023-05-create-movement
CREATE TABLE movement (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    movement_id    UUID          NOT NULL,
    account_number VARCHAR(20)   NOT NULL,
    movement_type  VARCHAR(10)   NOT NULL,
    amount         NUMERIC(19,2) NOT NULL,
    balance        NUMERIC(19,2) NOT NULL,
    movement_date  TIMESTAMPTZ   NOT NULL DEFAULT clock_timestamp(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_movement PRIMARY KEY (id),
    CONSTRAINT uk_movement_movement_id UNIQUE (movement_id),
    CONSTRAINT fk_movement_account FOREIGN KEY (account_number) REFERENCES account (account_number),
    CONSTRAINT ck_movement_amount CHECK (amount <> 0),
    CONSTRAINT ck_movement_balance CHECK (balance >= 0),
    CONSTRAINT ck_movement_type_sign CHECK (
        (movement_type = 'DEPOSIT' AND amount > 0) OR (movement_type = 'WITHDRAWAL' AND amount < 0))
);

CREATE INDEX ix_movement_account_number_id ON movement (account_number, id);
--rollback DROP TABLE movement;
