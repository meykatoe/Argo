import { ApiError } from '@/api/admin'

const TEXT: Record<string, string> = {
  ADMIN_UNAUTHORIZED: '令牌錯誤或後台未啟用',
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
