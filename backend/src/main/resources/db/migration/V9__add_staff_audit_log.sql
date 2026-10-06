-- 後台操作稽核紀錄，只增不改
create table staff_audit_log (
    id bigserial primary key,
    -- 失敗登入或系統操作時可為空
    staff_id bigint references staff_account (id),
    -- 操作當下的帳號快照
    username varchar(50) not null,
    role varchar(20),
    action varchar(50) not null,
    target_type varchar(30),
    target_id varchar(100),
    -- 變更前後的內容
    detail jsonb,
    success boolean not null default true,
    ip varchar(45),
    user_agent varchar(300),
    created_at timestamptz not null default now()
);

create index ix_audit_staff_time on staff_audit_log (staff_id, created_at desc);
create index ix_audit_time on staff_audit_log (created_at desc);
create index ix_audit_action_time on staff_audit_log (action, created_at desc);
create index ix_audit_target on staff_audit_log (target_type, target_id);

-- 禁止修改與刪除
create function forbid_audit_change() returns trigger language plpgsql as $$
begin
    raise exception 'staff_audit_log is append-only';
end;
$$;

create trigger trg_staff_audit_log_immutable
    before update or delete on staff_audit_log
    for each row execute function forbid_audit_change();
