import type { AuditAction, AuditLog, IpActivity, IpBlock, MenuNode, PageResult, Session } from '@/types'

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
  readonly details: Record<string, string>

  constructor(status: number, code: string, details: Record<string, string> = {}) {
    super(code)
    this.status = status
    this.code = code
    this.details = details
  }
}

async function request<T>(
  token: string | null,
  method: 'GET' | 'POST' | 'DELETE',
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
    const d = result?.data
    throw new ApiError(
      res.status,
      result?.msg ?? 'ERROR',
      d && typeof d === 'object' ? (d as Record<string, string>) : {},
    )
  }
  return result.data
}

export function login(username: string, password: string): Promise<Session> {
  return request(null, 'POST', '/ops/auth/login', {}, { username, password })
}

export function logout(token: string): Promise<void> {
  return request(token, 'POST', '/ops/auth/logout', {}, {})
}

export interface AuditSearch {
  username?: string
  action?: AuditAction | ''
  success?: boolean
  from?: string
  to?: string
  page: number
  size: number
}

export function searchAuditLogs(token: string, q: AuditSearch): Promise<PageResult<AuditLog>> {
  return request(token, 'GET', '/ops/audit-logs', { ...q })
}

export function getMenu(token: string): Promise<MenuNode[]> {
  return request(token, 'GET', '/ops/menu')
}

export interface IpSearch {
  days: number
  keyword?: string
  page: number
  size: number
}

export function listIps(token: string, q: IpSearch): Promise<PageResult<IpActivity>> {
  return request(token, 'GET', '/ops/ips', { ...q })
}

export function listIpBlocks(token: string): Promise<IpBlock[]> {
  return request(token, 'GET', '/ops/ip-blocks')
}

// hours 為空代表永久
export function blockIp(token: string, ip: string, reason: string, hours: number | null): Promise<IpBlock> {
  return request(token, 'POST', '/ops/ip-blocks', {}, { ip, reason, hours })
}

export function unblockIp(token: string, ip: string): Promise<void> {
  return request(token, 'DELETE', '/ops/ip-blocks', { ip })
}
