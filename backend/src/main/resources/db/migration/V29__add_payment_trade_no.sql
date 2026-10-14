-- 每次付款嘗試一個商店交易編號，綠界不接受重複
alter table payment add column trade_no varchar(20) unique;
alter table payment add column updated_at timestamptz not null default now();
