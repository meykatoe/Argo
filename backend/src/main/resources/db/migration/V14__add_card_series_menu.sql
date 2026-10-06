-- 卡牌系列頁，排在卡牌編輯之前
insert into admin_menu (parent_id, portal, code, title, path, sort_order)
values ((select id from admin_menu where code = 'card'), 'ADMIN', 'card.series', '卡牌系列', '/series', 5);

insert into role_menu (role, menu_id)
select r.role, m.id
from (values ('ADMIN'), ('GENERAL')) as r (role)
cross join admin_menu m
where m.code = 'card.series';
