-- 顧客帳號名稱，可用來登入，以小寫儲存且不可重複
alter table customer_account add column username varchar(30);
create unique index ux_customer_account_username on customer_account (username);
alter table customer_account add constraint ck_customer_username_lower
    check (username is null or username = lower(username));
