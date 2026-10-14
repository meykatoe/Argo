import { get, post } from './http'
import type { CreateOrderBody, Order, PayStart } from '@/types/order'

export function createOrder(body: CreateOrderBody) {
  return post<Order>('/orders', body)
}

export function getOrder(orderNo: string, email: string) {
  return get<Order>(`/orders/${encodeURIComponent(orderNo)}`, { email })
}

export function payOrder(orderNo: string, email: string) {
  return post<PayStart>(`/orders/${encodeURIComponent(orderNo)}/pay`, { email })
}

export function fakePay(orderNo: string, email: string, tradeNo: string, success: 0 | 1) {
  return post<Order>(`/orders/${encodeURIComponent(orderNo)}/fake-pay`, { email, tradeNo, success })
}

export function cancelOrder(orderNo: string, email: string) {
  return post<Order>(`/orders/${encodeURIComponent(orderNo)}/cancel`, { email })
}
