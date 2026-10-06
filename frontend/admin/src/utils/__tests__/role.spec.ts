import { describe, expect, it } from 'vitest'
import { canManageCards, roleLabel } from '../role'

describe('role', () => {
  it('卡片管理只開放 ADMIN 與 GENERAL', () => {
    expect(canManageCards('ADMIN')).toBe(true)
    expect(canManageCards('GENERAL')).toBe(true)
    expect(canManageCards('SERVICE')).toBe(false)
  })

  it('角色顯示中文名稱', () => {
    expect(roleLabel('ADMIN')).toBe('最高權限')
    expect(roleLabel('GENERAL')).toBe('一般管理員')
    expect(roleLabel('SERVICE')).toBe('客服')
  })
})
