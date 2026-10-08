import type { OrderStatus } from '@/types'

export const STATUS_TEXT: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待付款',
  PAID: '已付款',
  SHIPPED: '已出貨',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

export const STATUS_OPTIONS = Object.keys(STATUS_TEXT) as OrderStatus[]

const CANCEL_TEXT: Record<string, string> = {
  CUSTOMER: '顧客取消',
  EXPIRED: '逾期未付款',
}

export function cancelText(reason: string | null): string {
  return reason ? (CANCEL_TEXT[reason] ?? reason) : ''
}

export function formatTime(iso: string | null): string {
  if (!iso) {
    return '-'
  }
  const d = new Date(iso)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
