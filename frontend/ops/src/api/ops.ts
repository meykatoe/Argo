import { request } from '@shared/api'
import type { AuditAction, AuditLog, IpActivity, IpBlock, MenuNode, PageResult, Session } from '@/types'

export { ApiError, type Result } from '@shared/api'

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
  // 自動更新，後端不再逐次記錄查看
  refresh?: boolean
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
