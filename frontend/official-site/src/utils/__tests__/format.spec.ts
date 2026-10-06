import { afterEach, describe, expect, it } from 'vitest'
import { ApiError } from '@/api/http'
import { setLocale } from '@/i18n'
import { errorText } from '../error'
import { colorText, isAvailable, isOffShelf, typeText } from '../format'

describe('format', () => {
  afterEach(() => setLocale('zh-TW'))

  it('顏色與種類翻譯', () => {
    expect(colorText('Red Blue')).toBe('紅/藍')
    expect(typeText('Leader')).toBe('領航卡')
    setLocale('en')
    expect(colorText('Red Blue')).toBe('Red/Blue')
  })

  it('未知值保留原文', () => {
    expect(colorText('Pink')).toBe('Pink')
    expect(typeText('Don')).toBe('Don')
  })

  it('錯誤代碼轉訊息', () => {
    expect(errorText(new ApiError(404, 'CARD_NOT_FOUND'))).toBe('找不到這張卡片')
    expect(errorText(new ApiError(500, 'WHATEVER'))).toBe('發生未知錯誤，請稍後再試')
    expect(errorText(new TypeError('x'))).toBe('無法連線到伺服器，請稍後再試')
    setLocale('en')
    expect(errorText(new ApiError(404, 'CARD_NOT_FOUND'))).toBe('Card not found')
  })
  it('有庫存且已定價才可買', () => {
    expect(isAvailable({ stock: 2, salePrice: 1.5, onSale: 1 })).toBe(true)
    expect(isAvailable({ stock: 0, salePrice: 1.5, onSale: 1 })).toBe(false)
    expect(isAvailable({ stock: 2, salePrice: 0, onSale: 1 })).toBe(false)
  })

  it('系列下架時不可買，但仍可判斷為下架', () => {
    expect(isAvailable({ stock: 2, salePrice: 1.5, onSale: 0 })).toBe(false)
    expect(isOffShelf({ onSale: 0 })).toBe(true)
    expect(isOffShelf({ onSale: 1 })).toBe(false)
  })
})
