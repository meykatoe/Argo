-- 額外折扣，預設不打折
alter table card add column extra_discount numeric(5, 4) not null default 1;

alter table card add constraint ck_card_extra_discount check (extra_discount > 0 and extra_discount <= 1);
