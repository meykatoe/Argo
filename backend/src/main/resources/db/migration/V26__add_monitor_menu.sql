-- 系統管理選單
insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values (null, 'OPS', 'system', '系統管理', null, 30);

insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values ((select id from admin_menu where code = 'system'), 'OPS', 'system.monitor', '系統監控', '/monitor', 10);

insert into role_menu (role, menu_id)
select 'OPS', id from admin_menu where code = 'system.monitor';
