import { beforeEach, describe, expect, it } from 'vitest'
import { forgetOrderEmail, getOrderEmail, saveOrderEmail } from '../orderAccess'

describe('orderAccess', () => {
  beforeEach(() => sessionStorage.clear())

  it('依訂單編號記住信箱', () => {
    saveOrderEmail('A1', 'a@b.co')
    saveOrderEmail('A2', 'c@d.co')
    expect(getOrderEmail('A1')).toBe('a@b.co')
    expect(getOrderEmail('A2')).toBe('c@d.co')
    expect(getOrderEmail('A3')).toBe('')
  })

  it('可以忘記', () => {
    saveOrderEmail('A1', 'a@b.co')
    forgetOrderEmail('A1')
    expect(getOrderEmail('A1')).toBe('')
  })

  it('資料損毀時當作沒有', () => {
    sessionStorage.setItem('argo.orderAccess', '{壞掉')
    expect(getOrderEmail('A1')).toBe('')
  })
})
