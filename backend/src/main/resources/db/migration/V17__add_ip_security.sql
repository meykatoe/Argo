-- 每個來源 IP 每天的異常次數，由程式批次寫入
create table ip_activity (
    ip varchar(45) not null,
    day date not null,
    rate_limited integer not null default 0,
    login_failed integer not null default 0,
    blocked_hits integer not null default 0,
    first_seen timestamptz not null,
    last_seen timestamptz not null,
    primary key (ip, day)
);

create index ix_ip_activity_day on ip_activity (day);

-- 被封鎖的 IP，到期時間為空代表永久
create table ip_block (
    ip varchar(45) primary key,
    reason varchar(200) not null,
    blocked_by varchar(50) not null,
    staff_id bigint references staff_account (id),
    created_at timestamptz not null default now(),
    expires_at timestamptz
);

-- 運維後台選單：安全管理，檢視與封鎖分成兩個權限
insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values (null, 'OPS', 'security', '安全管理', null, 20);

insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values ((select id from admin_menu where code = 'security'), 'OPS', 'security.ips', 'IP 監控與封鎖', '/ips', 10),
       ((select id from admin_menu where code = 'security'), 'OPS', 'security.ips.block', '封鎖與解除封鎖 IP', null, 20);

insert into role_menu (role, menu_id)
select 'OPS', id from admin_menu where code in ('security.ips', 'security.ips.block');
