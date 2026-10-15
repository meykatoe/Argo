-- 退款狀態
alter table payment add column refund_status varchar(16) not null default 'NONE'
    check (refund_status in ('NONE', 'REQUIRED', 'REFUNDED'));
alter table payment add column refunded_at timestamptz;
alter table payment add column refunded_by bigint references staff_account (id);
create index payment_refund_status_idx on payment (refund_status) where refund_status <> 'NONE';
