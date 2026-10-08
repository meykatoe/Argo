import { request } from '@shared/api'
import type {
  AdminCard,
  AdminCardSet,
  AdminOrder,
  AdminOrderRow,
  Flag,
  MenuNode,
  OrderStatus,
  PageResult,
  Session,
  SetOption,
} from '@/types'

export { ApiError, type Result } from '@shared/api'

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

export function listCardSets(token: string): Promise<AdminCardSet[]> {
  return request(token, 'GET', '/admin/card-sets')
}

// 1 上架、0 下架
export function setCardSetOnSale(token: string, setId: string, onSale: Flag): Promise<AdminCardSet> {
  return request(token, 'PATCH', `/admin/card-sets/${encodeURIComponent(setId)}/on-sale`, {}, { onSale })
}

// 整個系列統一折扣
export function setCardSetDiscount(token: string, setId: string, extraDiscount: number): Promise<AdminCardSet> {
  return request(token, 'PATCH', `/admin/card-sets/${encodeURIComponent(setId)}/extra-discount`, {}, { extraDiscount })
}

// 公開 API，不需要令牌，取繁中系列名稱
export function listSetOptions(): Promise<SetOption[]> {
  return request(null, 'GET', '/sets', { lang: 'zh-TW' })
}

export interface OrderSearch {
  keyword?: string
  status?: OrderStatus
  page: number
  size: number
}

export function searchOrders(token: string, q: OrderSearch): Promise<PageResult<AdminOrderRow>> {
  return request(token, 'GET', '/admin/orders', { ...q })
}

export function getOrder(token: string, orderNo: string): Promise<AdminOrder> {
  return request(token, 'GET', `/admin/orders/${encodeURIComponent(orderNo)}`)
}

// 已付款訂單出貨，單號可留空
export function shipOrder(token: string, orderNo: string, trackingNo: string): Promise<AdminOrder> {
  return request(token, 'POST', `/admin/orders/${encodeURIComponent(orderNo)}/ship`, {}, { trackingNo })
}

export function completeOrder(token: string, orderNo: string): Promise<AdminOrder> {
  return request(token, 'POST', `/admin/orders/${encodeURIComponent(orderNo)}/complete`, {}, {})
}

export function cancelOrder(token: string, orderNo: string): Promise<AdminOrder> {
  return request(token, 'POST', `/admin/orders/${encodeURIComponent(orderNo)}/cancel`, {}, {})
}

export function setOrderNote(token: string, orderNo: string, note: string): Promise<AdminOrder> {
  return request(token, 'PATCH', `/admin/orders/${encodeURIComponent(orderNo)}/note`, {}, { note })
}
