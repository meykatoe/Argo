-- Email 驗證時間，空值代表尚未驗證
alter table customer_account add column email_verified_at timestamptz;

-- 驗證信與重設密碼共用，只存令牌雜湊
create table customer_email_token (
    token_hash varchar(64) primary key,
    customer_id bigint not null references customer_account (id) on delete cascade,
    purpose varchar(20) not null,
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    constraint ck_customer_email_token_purpose check (purpose in ('VERIFY', 'RESET'))
);

create index ix_customer_email_token_customer on customer_email_token (customer_id, purpose, created_at desc);
