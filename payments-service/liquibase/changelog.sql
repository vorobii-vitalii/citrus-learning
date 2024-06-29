--liquibase formatted sql

--changeset Vitalii:1 context:payment-service
--comment: Create table that stores balance of account in USD
create table if not exists user_balance (
    user_id      bigserial not null,
    user_balance numeric not null,
    primary key (user_id)
);
--rollback DROP TABLE user_balance;


--changeset Vitalii:2 context:payment-service
--comment: Create table that stores status of financial transaction
create table if not exists transactions (
    id uuid not null,
    status varchar(50) not null
);
--rollback DROP TABLE transactions;


--changeset Vitalii:3 context:payment-service
--comment: Create table that stores user information
create table if not exists user_info (
    user_id bigserial not null,
    is_blocked boolean not null
);
--rollback DROP TABLE user_info;
