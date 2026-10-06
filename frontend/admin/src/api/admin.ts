import type { AdminCard, PageResult, Session } from '@/types'

const BASE = import.meta.env.VITE_API_BASE ?? '/api'

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
      query.set(key, String(value))
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
  if (!res.ok) {
    let code = 'ERROR'
    try {
      code = (await res.json()).code ?? code
    } catch {
      // 非 JSON 回應
    }
    throw new ApiError(res.status, code)
  }
  // 無內容的回應
  if (res.status === 204) {
    return undefined as T
  }
  return (await res.json()) as T
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
