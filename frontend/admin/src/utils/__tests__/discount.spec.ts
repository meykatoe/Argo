import { describe, expect, it } from 'vitest'
import { discountLabel, discountRangeLabel, parseDiscount } from '../discount'

describe('parseDiscount', () => {
  it('接受合法折扣', () => {
    expect(parseDiscount('0.4')).toBe(0.4)
    expect(parseDiscount(' 1 ')).toBe(1)
    expect(parseDiscount('0.0001')).toBe(0.0001)
  })

  it('拒絕非法輸入', () => {
    for (const v of ['', '0', '1.1', '-1', '0.00001', 'abc', '.5', '1.0001', '0.4%']) {
      expect(parseDiscount(v)).toBeNull()
    }
  })
})

describe('discountLabel', () => {
  it('換算成折數', () => {
    expect(discountLabel(0.4)).toBe('4 折')
    expect(discountLabel(0.85)).toBe('8.5 折')
    expect(discountLabel(1)).toBe('無折扣')
  })
})

describe('discountRangeLabel', () => {
  it('相同顯示單一折扣，不同顯示範圍', () => {
    expect(discountRangeLabel(0.4, 0.4)).toBe('4 折')
    expect(discountRangeLabel(1, 1)).toBe('無折扣')
    expect(discountRangeLabel(0.4, 1)).toBe('不一致（4 折 ～ 無折扣）')
  })
})
