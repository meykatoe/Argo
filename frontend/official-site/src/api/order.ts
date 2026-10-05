import { get, post } from './http'
import type { CreateOrderBody, Order, PayBody } from '@/types/order'

export function createOrder(body: CreateOrderBody) {
  return post<Order>('/orders', body)
}

export function getOrder(orderNo: string, email: string) {
  return get<Order>(`/orders/${encodeURIComponent(orderNo)}`, { email })
}

export function payOrder(orderNo: string, body: PayBody) {
  return post<Order>(`/orders/${encodeURIComponent(orderNo)}/pay`, body)
}

export function cancelOrder(orderNo: string, email: string) {
  return post<Order>(`/orders/${encodeURIComponent(orderNo)}/cancel`, { email })
}
