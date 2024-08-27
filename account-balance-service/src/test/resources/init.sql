
-- User balance table
create table if not exists user_balance (
    user_id bigserial not null,
    user_balance  numeric not null,
    primary key (user_id)
);

