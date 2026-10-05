create table card_set (
    set_id      varchar(20) primary key,
    set_name    varchar(200) not null,
    category    varchar(20) not null default 'booster'
);

create table card (
    id                bigserial primary key,
    card_set_id       varchar(30) not null,
    card_image_id     varchar(40) not null,
    set_id            varchar(20) not null references card_set (set_id),
    card_name         varchar(200) not null,
    card_text         text,
    rarity            varchar(10) not null,
    card_color        varchar(50) not null,
    card_type         varchar(30) not null,
    card_cost         varchar(10),
    card_power        varchar(10),
    counter_amount    integer,
    life              varchar(10),
    attribute         varchar(50),
    sub_types         varchar(200),
    image_url         varchar(300),
    market_price      numeric(10, 2) not null default 0,
    inventory_price   numeric(10, 2) not null default 0,
    date_scraped      date,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now()
);

create index idx_card_set_id on card (set_id);
create index idx_card_card_set_id on card (card_set_id);
create index idx_card_image_id on card (card_image_id);
create index idx_card_name on card (card_name);
