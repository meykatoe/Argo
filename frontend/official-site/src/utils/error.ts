import { ApiError } from '@/api/http'
import { i18n } from '@/i18n'

// 錯誤轉成當前語言的訊息
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
