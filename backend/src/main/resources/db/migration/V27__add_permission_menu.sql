-- 權限管理選單
insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values ((select id from admin_menu where code = 'system'), 'OPS', 'system.permissions', '選單與權限', '/permissions', 20);

insert into role_menu (role, menu_id)
select 'OPS', id from admin_menu where code = 'system.permissions';
