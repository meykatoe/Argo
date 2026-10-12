-- 改為先驗證信箱才建立帳號，帳號有信箱就代表已驗證
alter table customer_account drop column email_verified_at;

delete from customer_email_token where purpose = 'VERIFY';
alter table customer_email_token drop constraint ck_customer_email_token_purpose;
alter table customer_email_token add constraint ck_customer_email_token_purpose check (purpose in ('RESET'));

-- 待驗證的註冊資料，驗證通過才轉成帳號，只存令牌雜湊與密碼雜湊
create table customer_pending_registration (
    token_hash varchar(64) primary key,
    username varchar(30) not null,
    email varchar(200) not null,
    password_hash varchar(100) not null,
    name varchar(100),
    expires_at timestamptz not null,
    created_at timestamptz not null default now()
);

create index ix_customer_pending_email on customer_pending_registration (email, created_at desc);
create index ix_customer_pending_expires on customer_pending_registration (expires_at);
