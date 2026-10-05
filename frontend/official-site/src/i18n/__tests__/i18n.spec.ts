import { afterEach, describe, expect, it, vi } from 'vitest'
import { get } from '@/api/http'
import { currentLocale, i18n, setLocale } from '@/i18n'
import en from '../locales/en'
import zhTW from '../locales/zh-TW'

// 攤平成 key 清單
function keys(obj: object, prefix = ''): string[] {
  return Object.entries(obj).flatMap(([k, v]) =>
    typeof v === 'object' ? keys(v, `${prefix}${k}.`) : [`${prefix}${k}`],
  )
}

describe('i18n', () => {
  afterEach(() => {
    setLocale('zh-TW')
    localStorage.clear()
    vi.unstubAllGlobals()
  })

  it('預設為繁體中文', () => {
    expect(currentLocale()).toBe('zh-TW')
    expect(i18n.global.t('nav.cards')).toBe('卡片')
  })

  it('切換語言並記住', () => {
    setLocale('en')
    expect(i18n.global.t('nav.cards')).toBe('Cards')
    expect(localStorage.getItem('argo.locale')).toBe('en')
    expect(document.documentElement.lang).toBe('en')
  })

  it('兩種語言鍵值一致', () => {
    expect(keys(en).sort()).toEqual(keys(zhTW).sort())
  })

  it('請求帶目前語言', async () => {
    const fn = vi.fn().mockResolvedValue(new Response('[]'))
    vi.stubGlobal('fetch', fn)
    setLocale('en')
    await get('/sets')
    expect(fn).toHaveBeenCalledWith('/api/sets?lang=en')
  })
})
