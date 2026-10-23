--liquibase formatted sql

--changeset account-service:20261023-02-create-account
CREATE TABLE account (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    account_number  VARCHAR(20)   NOT NULL,
    account_type    VARCHAR(10)   NOT NULL,
    initial_balance NUMERIC(19,2) NOT NULL,
    balance         NUMERIC(19,2) NOT NULL,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    customer_id     UUID          NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT uk_account_account_number UNIQUE (account_number),
    CONSTRAINT ck_account_account_type CHECK (account_type IN ('SAVINGS', 'CHECKING')),
    CONSTRAINT ck_account_initial_balance CHECK (initial_balance >= 0),
    CONSTRAINT fk_account_customer_snapshot FOREIGN KEY (customer_id) REFERENCES customer_snapshot (customer_id)
);

CREATE INDEX ix_account_customer_id ON account (customer_id);
--rollback DROP TABLE account;
