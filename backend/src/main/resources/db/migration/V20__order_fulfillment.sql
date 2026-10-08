-- 出貨與備註欄位
alter table shop_order add column tracking_no varchar(50);
alter table shop_order add column staff_note varchar(500);
alter table shop_order add column shipped_at timestamptz;
alter table shop_order add column completed_at timestamptz;

-- 訂單操作權限旗標，不對應頁面
insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values ((select id from admin_menu where code = 'order'), 'ADMIN', 'order.manage', '訂單處理', null, 20);

insert into role_menu (role, menu_id)
select r.role, m.id
from (values ('ADMIN'), ('GENERAL'), ('SERVICE')) as r (role)
cross join admin_menu m
where m.code = 'order.manage';
