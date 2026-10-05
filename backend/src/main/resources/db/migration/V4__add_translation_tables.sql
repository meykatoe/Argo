-- 卡片翻譯，以卡號對應
create table card_translation (
    id             bigserial primary key,
    card_set_id    varchar(30) not null,
    locale         varchar(10) not null,
    card_name      varchar(300) not null,
    card_text      text,
    sub_types      varchar(300),
    updated_at     timestamptz not null default now(),
    constraint uq_card_translation unique (card_set_id, locale)
);

-- 系列翻譯
create table card_set_translation (
    set_id      varchar(20) not null,
    locale      varchar(10) not null,
    set_name    varchar(300) not null,
    primary key (set_id, locale)
);
