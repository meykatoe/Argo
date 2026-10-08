export const AUTH_KEY = 'argo.auth'

export interface AuthSession {
  token: string
  email: string
  // 舊帳號沒有帳號名稱
  username: string | null
  name: string | null
  expiresAt: string
}

// 讀取時略過不合法或已過期的資料
export function readSession(): AuthSession | null {
  try {
    const raw = JSON.parse(localStorage.getItem(AUTH_KEY) ?? 'null')
    if (!raw || typeof raw.token !== 'string' || typeof raw.email !== 'string') return null
    if (!(new Date(raw.expiresAt).getTime() > Date.now())) return null
    return { token: raw.token, email: raw.email, username: raw.username ?? null, name: raw.name ?? null, expiresAt: raw.expiresAt }
  } catch {
    return null
  }
}

export function writeSession(s: AuthSession | null) {
  try {
    if (s) localStorage.setItem(AUTH_KEY, JSON.stringify(s))
    else localStorage.removeItem(AUTH_KEY)
  } catch {
    // 無法儲存就只在本次頁面有效
  }
}

// 供請求層帶上令牌
export function authToken(): string | null {
  return readSession()?.token ?? null
}
