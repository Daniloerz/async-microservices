--liquibase formatted sql

--changeset account-service:20261023-04-add-account-balance-check
ALTER TABLE account ADD CONSTRAINT ck_account_balance CHECK (balance >= 0);
--rollback ALTER TABLE account DROP CONSTRAINT ck_account_balance;
