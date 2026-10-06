-- 後台人員帳號，不開放註冊
create table staff_account (
    id bigserial primary key,
    username varchar(50) not null unique,
    password_hash varchar(100) not null,
    role varchar(20) not null,
    enabled boolean not null default true,
    failed_attempts integer not null default 0,
    locked_until timestamptz,
    created_at timestamptz not null default now(),
    last_login_at timestamptz,
    constraint ck_staff_role check (role in ('ADMIN', 'GENERAL', 'SERVICE'))
);

-- 只存令牌雜湊
create table staff_session (
    token_hash varchar(64) primary key,
    staff_id bigint not null references staff_account (id) on delete cascade,
    expires_at timestamptz not null,
    created_at timestamptz not null default now()
);

create index ix_staff_session_staff on staff_session (staff_id);
create index ix_staff_session_expires on staff_session (expires_at);
