import { get, post } from './http'
import type { Order } from '@/types/order'
import type { PageResult } from '@/types/card'
import type { AuthSession } from '@/utils/authToken'

export interface Me {
  email: string
  name: string | null
}

export function register(email: string, password: string, name: string) {
  return post<AuthSession>('/auth/register', { email, password, name: name || null })
}

export function login(email: string, password: string) {
  return post<AuthSession>('/auth/login', { email, password })
}

export function logout() {
  return post<null>('/auth/logout', {})
}

export function me() {
  return get<Me>('/auth/me')
}

export function myOrders(page = 1, size = 10) {
  return get<PageResult<Order>>('/me/orders', { page, size })
}
