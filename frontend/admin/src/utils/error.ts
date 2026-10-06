import { ApiError } from '@/api/admin'

const TEXT: Record<string, string> = {
  ADMIN_UNAUTHORIZED: '登入已失效，請重新登入',
  ADMIN_FORBIDDEN: '沒有權限執行此操作',
  LOGIN_FAILED: '帳號或密碼錯誤',
  LOGIN_LOCKED: '錯誤次數過多，請稍後再試',
  CARD_NOT_FOUND: '找不到這張卡片',
  VALIDATION_ERROR: '折扣必須大於 0 且不超過 1，最多四位小數',
  INVALID_PAGING: '分頁參數錯誤',
}

export function errorText(e: unknown): string {
  if (e instanceof ApiError) {
    return TEXT[e.code] ?? '發生未知錯誤'
  }
  return '無法連線到伺服器'
}
