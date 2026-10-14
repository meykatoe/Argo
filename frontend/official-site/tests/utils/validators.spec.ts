import { describe, expect, it } from 'vitest'
import {
  isEmail,
  isPhone,
  isPostalCode,
} from '@/utils/validators'

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
})
