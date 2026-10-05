import { createI18n } from 'vue-i18n'
import en from './locales/en'
import zhTW, { type MessageSchema } from './locales/zh-TW'

export const LOCALES = [
  { code: 'zh-TW', label: '繁體中文' },
  { code: 'en', label: 'English' },
] as const

export type LocaleCode = (typeof LOCALES)[number]['code']

const STORAGE_KEY = 'argo.locale'
const DEFAULT_LOCALE: LocaleCode = 'zh-TW'

function isLocale(v: unknown): v is LocaleCode {
  return LOCALES.some((l) => l.code === v)
}

// 優先用上次的選擇
function initialLocale(): LocaleCode {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (isLocale(saved)) return saved
  } catch {
    // 無法讀取就用預設
  }
  return DEFAULT_LOCALE
}

export const i18n = createI18n<[MessageSchema], LocaleCode, false>({
  legacy: false,
  locale: initialLocale(),
  fallbackLocale: 'en',
  messages: { 'zh-TW': zhTW, en },
})

export function currentLocale(): LocaleCode {
  return i18n.global.locale.value
}

export function setLocale(code: LocaleCode) {
  i18n.global.locale.value = code
  document.documentElement.lang = code
  try {
    localStorage.setItem(STORAGE_KEY, code)
  } catch {
    // 儲存失敗不影響使用
  }
}

document.documentElement.lang = currentLocale()
