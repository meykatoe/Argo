-- 註冊也可輸入 6 位數驗證碼，只存雜湊，錯誤次數過多就作廢
delete from customer_pending_registration;
alter table customer_pending_registration add column code_hash varchar(64) not null;
alter table customer_pending_registration add column code_expires_at timestamptz not null;
alter table customer_pending_registration add column code_attempts integer not null default 0;
