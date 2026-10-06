import { describe, expect, it } from 'vitest'
import type { AuditLog } from '@/types'
import { actionLabel, summarize, toIso } from '../audit'

function log(p: Partial<AuditLog>): AuditLog {
  return {
    id: 1,
    staffId: 1,
    username: 'gen1',
    role: 'GENERAL',
    action: 'LOGIN_SUCCESS',
    targetType: null,
    targetId: null,
    detail: null,
    success: 1,
    ip: '127.0.0.1',
    userAgent: null,
    createdAt: '2026-10-06T03:00:00Z',
    ...p,
  }
}

describe('audit utils', () => {
  it('動作顯示中文，未知動作原樣顯示', () => {
    expect(actionLabel('CARD_EXTRA_DISCOUNT_UPDATE')).toBe('修改額外折扣')
    expect(actionLabel('SOMETHING_NEW')).toBe('SOMETHING_NEW')
  })

  it('折扣修改摘要包含前後變化', () => {
    const s = summarize(
      log({
        action: 'CARD_EXTRA_DISCOUNT_UPDATE',
        detail: {
          cardSetId: 'OP01-001',
          extraDiscountBefore: '1',
          extraDiscountAfter: '0.4',
          salePriceBefore: '9.00',
          salePriceAfter: '3.60',
        },
      }),
    )
    expect(s).toBe('OP01-001 折扣 1 → 0.4，售價 9.00 → 3.60')
  })

  it('登入失敗顯示原因', () => {
    expect(summarize(log({ action: 'LOGIN_FAILED', detail: { reason: 'BAD_PASSWORD' } }))).toBe('密碼錯誤')
    expect(summarize(log({ action: 'LOGIN_FAILED', detail: { reason: 'WRONG_PORTAL' } }))).toBe('走錯登入入口')
  })

  it('越權摘要顯示方法與路徑', () => {
    expect(
      summarize(log({ action: 'ACCESS_DENIED', targetId: '/api/admin/cards', detail: { method: 'GET' } })),
    ).toBe('GET /api/admin/cards')
  })

  it('空白時間不帶條件，有值轉為 ISO', () => {
    expect(toIso('')).toBeUndefined()
    expect(toIso('not-a-date')).toBeUndefined()
    expect(toIso('2026-10-06T12:00')).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/)
  })
})
