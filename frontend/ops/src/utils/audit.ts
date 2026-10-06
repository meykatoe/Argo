import type { AuditAction, AuditLog } from '@/types'
import { roleName } from './role'

export const ACTION_LABELS: Record<AuditAction, string> = {
  LOGIN_SUCCESS: '登入成功',
  LOGIN_FAILED: '登入失敗',
  LOGIN_LOCKED: '登入被鎖定',
  LOGOUT: '登出',
  ACCESS_DENIED: '越權被拒',
  ACCOUNT_CREATED: '建立帳號',
  CARD_EXTRA_DISCOUNT_UPDATE: '修改額外折扣',
  AUDIT_LOG_VIEWED: '查看稽核紀錄',
}

const REASONS: Record<string, string> = {
  UNKNOWN_USER: '帳號不存在',
  BAD_PASSWORD: '密碼錯誤',
  DISABLED: '帳號已停用',
  WRONG_PORTAL: '走錯登入入口',
}

export function actionLabel(action: string): string {
  return ACTION_LABELS[action as AuditAction] ?? action
}

export function roleLabel(role: string | null): string {
  return role ? roleName(role) : '-'
}

// 本地時間顯示
export function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('zh-TW', { hour12: false })
}

// 本地輸入值轉為 ISO，空值回傳 undefined
export function toIso(local: string): string | undefined {
  if (!local) {
    return undefined
  }
  const d = new Date(local)
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString()
}

function str(v: unknown): string {
  return v === undefined || v === null ? '' : String(v)
}

// 一行摘要，看不懂的動作回傳空字串
export function summarize(log: AuditLog): string {
  const d = log.detail ?? {}
  switch (log.action) {
    case 'CARD_EXTRA_DISCOUNT_UPDATE':
      return `${str(d.cardSetId)} 折扣 ${str(d.extraDiscountBefore)} → ${str(d.extraDiscountAfter)}，售價 ${str(d.salePriceBefore)} → ${str(d.salePriceAfter)}`
    case 'LOGIN_FAILED':
      return REASONS[str(d.reason)] ?? str(d.reason)
    case 'ACCESS_DENIED':
      return `${str(d.method)} ${str(log.targetId)}`.trim()
    case 'ACCOUNT_CREATED':
      return `${str(log.targetId)}（${roleLabel(str(d.role) || null)}）`
    default:
      return ''
  }
}
