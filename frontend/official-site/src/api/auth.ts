import { get, post } from './http'
import type { Order } from '@/types/order'
import type { PageResult } from '@/types/card'
import type { AuthSession } from '@/utils/authToken'

export interface Me {
  email: string
  username: string | null
  name: string | null
}

// 只寄出驗證信，驗證通過才會建立帳號
export function register(username: string, email: string, password: string, name: string) {
  return post<null>('/auth/register', { username, email, password, name: name || null })
}

// 帳號名稱或 Email 皆可
export function login(account: string, password: string) {
  return post<AuthSession>('/auth/login', { account, password })
}

// 1 可用、0 已被使用
export function usernameAvailable(username: string) {
  return get<{ available: 0 | 1 }>('/auth/username-available', { username })
}

// 點信中連結驗證，通過即登入
export function verifyEmail(token: string) {
  return post<AuthSession>('/auth/verify-email', { token })
}

// 輸入信中 6 位數驗證碼，通過即登入
export function verifyCode(email: string, code: string) {
  return post<AuthSession>('/auth/verify-code', { email, code })
}

export function resendVerification(email: string) {
  return post<null>('/auth/resend-verification', { email })
}

// 不論信箱是否註冊過都會成功
export function forgotPassword(email: string) {
  return post<null>('/auth/forgot-password', { email })
}

export function resetPassword(token: string, password: string) {
  return post<null>('/auth/reset-password', { token, password })
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

// 同 Email 還沒綁帳號的訪客訂單數
export function claimableOrders() {
  return get<{ count: number }>('/me/orders/claimable')
}

export function claimOrders() {
  return post<{ count: number }>('/me/orders/claim', {})
}

export function myOrders(page = 1, size = 10) {
  return get<PageResult<Order>>('/me/orders', { page, size })
}
