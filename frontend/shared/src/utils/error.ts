import { ApiError } from '../api'

// 兩個後台共用的錯誤文字
const BASE: Record<string, string> = {
  ADMIN_UNAUTHORIZED: '登入已失效，請重新登入',
  ADMIN_FORBIDDEN: '沒有權限執行此操作',
  LOGIN_FAILED: '帳號或密碼錯誤',
  LOGIN_LOCKED: '錯誤次數過多，請稍後再試',
  INVALID_PAGING: '分頁參數錯誤',
  RATE_LIMITED: '操作太頻繁，請稍後再試',
  IP_BLOCKED: '你的網路位址已被限制存取',
}

export interface ErrorTexts {
  errorText: (e: unknown) => string
  lockedMessage: (e: unknown) => string | null
}

// 各後台補上自己的錯誤文字
export function createErrorTexts(extra: Record<string, string> = {}): ErrorTexts {
  const texts = { ...BASE, ...extra }
  return {
    errorText(e) {
      if (e instanceof ApiError) {
        return texts[e.code] ?? '發生未知錯誤'
      }
      return '無法連線到伺服器'
    },
    // 鎖定時回應會帶還要等幾秒，換成分鐘顯示
    lockedMessage(e) {
      if (e instanceof ApiError && e.code === 'LOGIN_LOCKED') {
        const seconds = Number(e.details.retryAfterSeconds)
        if (Number.isFinite(seconds) && seconds > 0) {
          return `密碼錯誤次數過多，帳號已暫時鎖定，請 ${Math.ceil(seconds / 60)} 分鐘後再試`
        }
      }
      return null
    },
  }
}
