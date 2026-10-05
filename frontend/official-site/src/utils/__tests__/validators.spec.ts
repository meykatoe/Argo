import { describe, expect, it } from 'vitest'
import {
  formatCardNumber,
  isCardExpired,
  isCardNumber,
  isCvc,
  isEmail,
  isPhone,
  isPostalCode,
} from '../validators'

describe('validators', () => {
  it('Email', () => {
    expect(isEmail('a@b.co')).toBe(true)
    expect(isEmail(' a@b.co ')).toBe(true)
    expect(isEmail('a@b')).toBe(false)
    expect(isEmail('not mail')).toBe(false)
  })

  it('電話', () => {
    expect(isPhone('0912345678')).toBe(true)
    expect(isPhone('02-2345-6789')).toBe(true)
    expect(isPhone('+886 912 345 678')).toBe(true)
    expect(isPhone('123')).toBe(false)
    expect(isPhone('abcdefghij')).toBe(false)
  })

  it('郵遞區號', () => {
    expect(isPostalCode('100')).toBe(true)
    expect(isPostalCode('10058')).toBe(true)
    expect(isPostalCode('100580')).toBe(true)
    expect(isPostalCode('10')).toBe(false)
    expect(isPostalCode('1005')).toBe(false)
  })

  it('卡號檢查碼', () => {
    expect(isCardNumber('4242 4242 4242 4242')).toBe(true)
    expect(isCardNumber('4242-4242-4242-4242')).toBe(true)
    expect(isCardNumber('4242 4242 4242 4241')).toBe(false)
    expect(isCardNumber('1234')).toBe(false)
  })

  it('有效期限', () => {
    const now = new Date(2026, 9, 5)
    expect(isCardExpired(9, 2026, now)).toBe(true)
    expect(isCardExpired(10, 2026, now)).toBe(false)
    expect(isCardExpired(1, 2025, now)).toBe(true)
    expect(isCardExpired(1, 2027, now)).toBe(false)
  })

  it('安全碼', () => {
    expect(isCvc('123')).toBe(true)
    expect(isCvc('1234')).toBe(true)
    expect(isCvc('12')).toBe(false)
    expect(isCvc('12a')).toBe(false)
  })

  it('卡號格式化', () => {
    expect(formatCardNumber('4242424242424242')).toBe('4242 4242 4242 4242')
    expect(formatCardNumber('42a4 24')).toBe('4242 4')
  })
})
