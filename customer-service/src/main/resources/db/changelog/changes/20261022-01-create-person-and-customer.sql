--liquibase formatted sql

--changeset customer-service:20261022-01-create-person-and-customer
CREATE TABLE person (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    name           VARCHAR(100) NOT NULL,
    gender         VARCHAR(10)  NOT NULL,
    age            SMALLINT     NOT NULL,
    identification VARCHAR(20)  NOT NULL,
    address        VARCHAR(200) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_person PRIMARY KEY (id),
    CONSTRAINT uk_person_identification UNIQUE (identification),
    CONSTRAINT ck_person_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_person_age CHECK (age BETWEEN 18 AND 120)
);

CREATE TABLE customer (
    id            BIGINT       NOT NULL,
    customer_id   UUID         NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_customer PRIMARY KEY (id),
    CONSTRAINT uk_customer_customer_id UNIQUE (customer_id),
    CONSTRAINT fk_customer_person FOREIGN KEY (id) REFERENCES person (id) ON DELETE CASCADE
);
--rollback DROP TABLE customer; DROP TABLE person;
