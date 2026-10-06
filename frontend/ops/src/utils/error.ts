import { ApiError } from '@/api/ops'

const TEXT: Record<string, string> = {
  ADMIN_UNAUTHORIZED: '登入已失效，請重新登入',
  ADMIN_FORBIDDEN: '沒有權限執行此操作',
  LOGIN_FAILED: '帳號或密碼錯誤',
  LOGIN_LOCKED: '錯誤次數過多，請稍後再試',
  INVALID_PAGING: '分頁參數錯誤',
  INVALID_RANGE: '開始時間必須早於結束時間',
  BAD_REQUEST: '查詢條件有誤',
}

export function errorText(e: unknown): string {
  if (e instanceof ApiError) {
    return TEXT[e.code] ?? '發生未知錯誤'
  }
  return '無法連線到伺服器'
}
