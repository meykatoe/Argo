-- 官網顧客帳號，可自行註冊，也保留訪客下單
create table customer_account (
    id bigserial primary key,
    email varchar(200) not null unique,
    password_hash varchar(100) not null,
    name varchar(100),
    -- 0 正常使用、1 停用
    disabled smallint not null default 0,
    failed_attempts integer not null default 0,
    locked_until timestamptz,
    created_at timestamptz not null default now(),
    last_login_at timestamptz,
    constraint ck_customer_account_disabled check (disabled in (0, 1))
);

-- 只存令牌雜湊
create table customer_session (
    token_hash varchar(64) primary key,
    customer_id bigint not null references customer_account (id) on delete cascade,
    expires_at timestamptz not null,
    created_at timestamptz not null default now()
);

create index ix_customer_session_customer on customer_session (customer_id);
create index ix_customer_session_expires on customer_session (expires_at);

-- 訂單可綁定顧客，訪客訂單為空
alter table shop_order add column customer_id bigint references customer_account (id);
create index ix_shop_order_customer on shop_order (customer_id, created_at desc);
