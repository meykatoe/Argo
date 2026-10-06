-- 系列上下架，下架後官網仍顯示但不販售
alter table card_set add column on_sale boolean not null default true;
