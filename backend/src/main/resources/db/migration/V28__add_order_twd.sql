-- 下單當下的匯率與新台幣應付金額，舊訂單為空
alter table shop_order add column fx_rate numeric(12, 6);
alter table shop_order add column total_twd bigint;
alter table shop_order add constraint ck_shop_order_total_twd check (total_twd is null or total_twd > 0);
