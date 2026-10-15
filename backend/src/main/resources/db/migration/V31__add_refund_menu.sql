-- 退款處理選單
insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values (null, 'OPS', 'payment', '付款管理', null, 20);

insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values ((select id from admin_menu where code = 'payment'), 'OPS', 'payment.refunds', '退款處理', '/refunds', 10);

insert into role_menu (role, menu_id)
select 'OPS', id from admin_menu where code in ('payment', 'payment.refunds');
