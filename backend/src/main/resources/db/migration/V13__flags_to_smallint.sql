-- 是否類欄位統一存 0 與 1
alter table card alter column price_overridden drop default;
alter table card alter column price_overridden type smallint using (case when price_overridden then 1 else 0 end);
alter table card alter column price_overridden set default 0;
alter table card add constraint ck_card_price_overridden check (price_overridden in (0, 1));

alter table card_set alter column on_sale drop default;
alter table card_set alter column on_sale type smallint using (case when on_sale then 1 else 0 end);
alter table card_set alter column on_sale set default 1;
alter table card_set add constraint ck_card_set_on_sale check (on_sale in (0, 1));

alter table staff_account alter column enabled drop default;
alter table staff_account alter column enabled type smallint using (case when enabled then 1 else 0 end);
alter table staff_account alter column enabled set default 1;
alter table staff_account add constraint ck_staff_account_enabled check (enabled in (0, 1));

alter table admin_menu alter column enabled drop default;
alter table admin_menu alter column enabled type smallint using (case when enabled then 1 else 0 end);
alter table admin_menu alter column enabled set default 1;
alter table admin_menu add constraint ck_admin_menu_enabled check (enabled in (0, 1));

alter table staff_audit_log alter column success drop default;
alter table staff_audit_log alter column success type smallint using (case when success then 1 else 0 end);
alter table staff_audit_log alter column success set default 1;
alter table staff_audit_log add constraint ck_staff_audit_log_success check (success in (0, 1));
