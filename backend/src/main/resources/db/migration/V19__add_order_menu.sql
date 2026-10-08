-- 訂單管理選單
insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values (null, 'ADMIN', 'order', '訂單管理', null, 20);

insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values ((select id from admin_menu where code = 'order'), 'ADMIN', 'order.list', '訂單列表', '/orders', 10);

insert into role_menu (role, menu_id)
select r.role, m.id
from (values ('ADMIN'), ('GENERAL'), ('SERVICE')) as r (role)
cross join admin_menu m
where m.code = 'order.list';
