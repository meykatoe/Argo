-- 售價獨立存放
alter table card add column sale_price numeric(10, 2) not null default 0;
-- 手動改價不被覆蓋
alter table card add column price_overridden boolean not null default false;
alter table card add column stock integer not null default 0;

update card set sale_price = round(market_price * 0.9, 2);

alter table card add constraint ck_card_stock check (stock >= 0);
