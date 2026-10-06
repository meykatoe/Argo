-- 帳號是否停用：0 正常使用、1 停用，取代原本語意相反的 enabled
alter table staff_account add column disabled smallint not null default 0;
update staff_account set disabled = case when enabled = 1 then 0 else 1 end;
alter table staff_account add constraint ck_staff_account_disabled check (disabled in (0, 1));
alter table staff_account drop column enabled;
