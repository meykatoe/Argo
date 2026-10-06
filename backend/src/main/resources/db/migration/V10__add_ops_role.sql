-- 新增運維角色
alter table staff_account drop constraint ck_staff_role;
alter table staff_account add constraint ck_staff_role
    check (role in ('ADMIN', 'GENERAL', 'SERVICE', 'OPS'));
