-- 自動封鎖規則：每種事件一條，次數在時間內達標就封鎖
create table ip_auto_block_rule (
    id bigserial primary key,
    metric varchar(20) not null unique,
    -- 0 停用、1 啟用
    enabled smallint not null default 1,
    threshold integer not null,
    window_minutes integer not null,
    block_hours integer not null,
    updated_at timestamptz not null default now(),
    updated_by varchar(50),
    constraint ck_ip_rule_metric check (metric in ('RATE_LIMITED', 'LOGIN_FAILED')),
    constraint ck_ip_rule_enabled check (enabled in (0, 1)),
    constraint ck_ip_rule_threshold check (threshold between 2 and 10000),
    constraint ck_ip_rule_window check (window_minutes between 1 and 1440),
    constraint ck_ip_rule_hours check (block_hours between 1 and 8760)
);

-- 預設規則：5 分鐘內被限速 30 次，或 10 分鐘內登入失敗 40 次，封鎖 1 小時
insert into ip_auto_block_rule (metric, enabled, threshold, window_minutes, block_hours)
values ('RATE_LIMITED', 1, 30, 5, 1),
       ('LOGIN_FAILED', 1, 40, 10, 1);

-- 白名單：不會被自動封鎖，可以是單一 IP 或 CIDR
create table ip_allowlist (
    ip varchar(49) primary key,
    note varchar(200) not null,
    created_by varchar(50) not null,
    created_at timestamptz not null default now()
);

-- 區分自動封鎖與人工封鎖
alter table ip_block add column auto smallint not null default 0;
alter table ip_block add constraint ck_ip_block_auto check (auto in (0, 1));

-- 運維選單：自動封鎖規則，檢視與修改分成兩個權限
insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values ((select id from admin_menu where code = 'security'), 'OPS', 'security.rules', '自動封鎖規則', '/ip-rules', 30),
       ((select id from admin_menu where code = 'security'), 'OPS', 'security.rules.edit', '修改自動封鎖規則與白名單', null, 40);

insert into role_menu (role, menu_id)
select 'OPS', id from admin_menu where code in ('security.rules', 'security.rules.edit');
