-- 後台選單，同時作為功能權限的節點
create table admin_menu (
    id bigserial primary key,
    parent_id bigint references admin_menu (id),
    portal varchar(10) not null,
    -- 權限代碼，程式以此判斷權限
    code varchar(60) not null unique,
    title varchar(60) not null,
    -- 前端路徑，群組節點為空
    path varchar(100),
    sort_order integer not null default 0,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    constraint ck_admin_menu_portal check (portal in ('ADMIN', 'OPS'))
);

create index ix_admin_menu_parent on admin_menu (parent_id);

-- 哪個角色可使用哪個節點
create table role_menu (
    role varchar(20) not null,
    menu_id bigint not null references admin_menu (id) on delete cascade,
    primary key (role, menu_id)
);

insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values (null, 'ADMIN', 'card', '卡牌管理', null, 10),
       (null, 'OPS', 'audit', '稽核管理', null, 10);

insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values ((select id from admin_menu where code = 'card'), 'ADMIN', 'card.edit', '卡牌編輯', '/cards', 10),
       ((select id from admin_menu where code = 'audit'), 'OPS', 'audit.logs', '稽核紀錄', '/audit-logs', 10);

-- 沿用目前的權限配置
insert into role_menu (role, menu_id)
select r.role, m.id
from (values ('ADMIN', 'card.edit'), ('GENERAL', 'card.edit'), ('OPS', 'audit.logs')) as r (role, code)
join admin_menu m on m.code = r.code;
