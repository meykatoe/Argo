-- 訂單主檔
create table shop_order (
    id               bigserial primary key,
    order_no         varchar(20) not null unique,
    status           varchar(20) not null,
    currency         varchar(3) not null,
    subtotal         numeric(12, 2) not null,
    shipping_fee     numeric(12, 2) not null,
    total            numeric(12, 2) not null,
    customer_name    varchar(100) not null,
    customer_email   varchar(200) not null,
    customer_phone   varchar(30) not null,
    recipient_name   varchar(100) not null,
    recipient_phone  varchar(30) not null,
    postal_code      varchar(10) not null,
    city             varchar(50) not null,
    address          varchar(200) not null,
    lang             varchar(10) not null,
    cancel_reason    varchar(20),
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    paid_at          timestamptz,
    cancelled_at     timestamptz
);

create index idx_shop_order_status_created on shop_order (status, created_at);
create index idx_shop_order_email on shop_order (customer_email);

-- 明細保留下單當下的快照
create table shop_order_item (
    id             bigserial primary key,
    order_id       bigint not null references shop_order (id),
    card_id        bigint not null,
    card_set_id    varchar(30) not null,
    card_name      varchar(300) not null,
    card_name_en   varchar(300) not null,
    image_url      varchar(500),
    unit_price     numeric(10, 2) not null,
    quantity       integer not null check (quantity > 0),
    subtotal       numeric(12, 2) not null
);

create index idx_shop_order_item_order on shop_order_item (order_id);

-- 只存卡號末四碼
create table payment (
    id              bigserial primary key,
    order_id        bigint not null references shop_order (id),
    method          varchar(20) not null,
    status          varchar(20) not null,
    amount          numeric(12, 2) not null,
    currency        varchar(3) not null,
    card_last4      varchar(4),
    transaction_id  varchar(40),
    failure_code    varchar(30),
    created_at      timestamptz not null default now()
);

create index idx_payment_order on payment (order_id);
