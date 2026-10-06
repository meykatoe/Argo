import type { AdminCard, MenuNode, PageResult, Session } from '@/types'

const BASE = import.meta.env.VITE_API_BASE ?? '/api'

// 後端統一回傳格式
export interface Result<T> {
  code: number
  msg: string
  data: T
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string) {
    super(code)
    this.status = status
    this.code = code
  }
}

async function request<T>(
  token: string | null,
  method: 'GET' | 'POST' | 'PATCH',
  path: string,
  params: Record<string, string | number | boolean | undefined> = {},
  body?: unknown,
): Promise<T> {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    // 略過空值參數
    if (value !== undefined && value !== '') {
      // 是否類參數一律送 1 或 0
      query.set(key, typeof value === 'boolean' ? (value ? '1' : '0') : String(value))
    }
  }
  const headers: Record<string, string> = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const res = await fetch(`${BASE}${path}?${query.toString()}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  let result: Result<T> | null = null
  try {
    const body = await res.json()
    result = body && typeof body === 'object' && typeof body.code === 'number' ? body : null
  } catch {
    // 非 JSON 回應
  }
  // 成功代碼固定為 200，失敗時 msg 為錯誤代碼
  if (!res.ok || result?.code !== 200) {
    throw new ApiError(res.status, result?.msg ?? 'ERROR')
  }
  return result.data
}

export function login(username: string, password: string): Promise<Session> {
  return request(null, 'POST', '/admin/auth/login', {}, { username, password })
}

export function logout(token: string): Promise<void> {
  return request(token, 'POST', '/admin/auth/logout', {}, {})
}

export interface CardSearch {
  keyword?: string
  setId?: string
  discounted?: boolean
  page: number
  size: number
}

export function searchCards(token: string, q: CardSearch): Promise<PageResult<AdminCard>> {
  return request(token, 'GET', '/admin/cards', { ...q })
}

export function setExtraDiscount(token: string, id: number, extraDiscount: number): Promise<AdminCard> {
  return request(token, 'PATCH', `/admin/cards/${id}/extra-discount`, {}, { extraDiscount })
}

export function getMenu(token: string): Promise<MenuNode[]> {
  return request(token, 'GET', '/admin/menu')
}
