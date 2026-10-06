import { ApiError } from '@/api/http'
import { i18n } from '@/i18n'

// 錯誤轉成當前語言的訊息
// 鎖定時回應會帶還要等幾秒，換成分鐘顯示
export function lockedMinutes(e: unknown): number | null {
  if (e instanceof ApiError && e.code === 'LOGIN_LOCKED') {
    const seconds = Number(e.details.retryAfterSeconds)
    if (Number.isFinite(seconds) && seconds > 0) return Math.ceil(seconds / 60)
  }
  return null
}

export function errorText(e: unknown): string {
  const { t, te } = i18n.global
  if (e instanceof ApiError) {
    return te(`error.${e.code}`) ? t(`error.${e.code}`) : t('error.unknown')
  }
  if (e instanceof TypeError) {
    return t('error.network')
  }
  return t('error.unknown')
}
