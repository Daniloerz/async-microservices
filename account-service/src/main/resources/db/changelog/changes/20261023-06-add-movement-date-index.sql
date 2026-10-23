--liquibase formatted sql

--changeset account-service:20261023-06-add-movement-date-index
CREATE INDEX ix_movement_account_number_movement_date ON movement (account_number, movement_date);
--rollback DROP INDEX ix_movement_account_number_movement_date;
