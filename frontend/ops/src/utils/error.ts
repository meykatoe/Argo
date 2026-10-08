import { ApiError } from '@/api/ops'

const TEXT: Record<string, string> = {
  ADMIN_UNAUTHORIZED: '登入已失效，請重新登入',
  ADMIN_FORBIDDEN: '沒有權限執行此操作',
  LOGIN_FAILED: '帳號或密碼錯誤',
  LOGIN_LOCKED: '錯誤次數過多，請稍後再試',
  INVALID_PAGING: '分頁參數錯誤',
  RATE_LIMITED: '操作太頻繁，請稍後再試',
  PROTECTED_IP: '這個位址不可封鎖（本機、代理或系統保留位址）',
  CANNOT_BLOCK_SELF: '不能封鎖你自己目前使用的位址',
  INVALID_IP: 'IP 格式不正確',
  IP_NOT_BLOCKED: '這個 IP 目前沒有被封鎖',
  IP_BLOCKED: '你的網路位址已被限制存取',
  INVALID_RANGE: '開始時間必須早於結束時間',
  ORDER_STATE_CONFLICT: '訂單狀態已變更，請重新整理後再試',
  BAD_REQUEST: '查詢條件有誤',
}

// 鎖定時回應會帶還要等幾秒，換成分鐘顯示
export function lockedMessage(e: unknown): string | null {
  if (e instanceof ApiError && e.code === 'LOGIN_LOCKED') {
    const seconds = Number(e.details.retryAfterSeconds)
    if (Number.isFinite(seconds) && seconds > 0) {
      return `密碼錯誤次數過多，帳號已暫時鎖定，請 ${Math.ceil(seconds / 60)} 分鐘後再試`
    }
  }
  return null
}

export function errorText(e: unknown): string {
  if (e instanceof ApiError) {
    return TEXT[e.code] ?? '發生未知錯誤'
  }
  return '無法連線到伺服器'
}
