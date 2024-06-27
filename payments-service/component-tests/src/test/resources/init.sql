
-- User balance table
create table if not exists user_balance (
    user_id bigserial not null,
    user_balance  numeric not null,
    primary key (user_id)
);

-- Transactions table
create table if not exists transactions (
    id uuid not null,
    status varchar(50) not null
);

-- User info table
create table if not exists user_info (
    user_id bigserial not null,
    is_blocked boolean not null
);
