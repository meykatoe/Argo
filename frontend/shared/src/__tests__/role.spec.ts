import { describe, expect, it } from 'vitest'
import { roleName } from '../utils/role'

describe('roleName', () => {
  it('角色顯示中文名稱，未知角色原樣顯示', () => {
    expect(roleName('ADMIN')).toBe('最高權限')
    expect(roleName('GENERAL')).toBe('一般管理員')
    expect(roleName('SERVICE')).toBe('客服')
    expect(roleName('OPS')).toBe('運維')
    expect(roleName('NEW')).toBe('NEW')
  })
})
