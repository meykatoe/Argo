import { get, post } from './http'
import type { Order } from '@/types/order'
import type { PageResult } from '@/types/card'
import type { AuthSession } from '@/utils/authToken'

export interface Me {
  email: string
  username: string | null
  name: string | null
}

export function register(username: string, email: string, password: string, name: string) {
  return post<AuthSession>('/auth/register', { username, email, password, name: name || null })
}

// 帳號名稱或 Email 皆可
export function login(account: string, password: string) {
  return post<AuthSession>('/auth/login', { account, password })
}

// 1 可用、0 已被使用
export function usernameAvailable(username: string) {
  return get<{ available: 0 | 1 }>('/auth/username-available', { username })
}

export function logout() {
  return post<null>('/auth/logout', {})
}

// 啟動時確認這個 IP 有沒有被封鎖
export function ping() {
  return get<null>('/ping')
}

export function me() {
  return get<Me>('/auth/me')
}

export function myOrders(page = 1, size = 10) {
  return get<PageResult<Order>>('/me/orders', { page, size })
}
