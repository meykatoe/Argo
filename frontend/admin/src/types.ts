export type { Flag, MenuNode, PageResult, Session, StaffRole } from '@shared/types'
import type { Flag, StaffRole } from '@shared/types'

export interface AdminCard {
  id: number
  cardSetId: string
  cardName: string
  cardNameEn: string
  rarity: string
  imageUrl: string | null
  marketPrice: number
  listPrice: number
  extraDiscount: number
  salePrice: number
  priceOverridden: Flag
  stock: number
}

export interface AdminCardSet {
  setId: string
  setName: string
  setNameEn: string
  category: string
  onSale: Flag
  cardCount: number
  minDiscount: number
  maxDiscount: number
}

// 公開的系列清單，用於下拉選單
export interface SetOption {
  setId: string
  setName: string
  category: string
  onSale: Flag
}

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED'

export interface AdminOrderRow {
  orderNo: string
  status: OrderStatus
  currency: string
  total: number
  customerName: string
  customerEmail: string
  itemCount: number
  createdAt: string
  paidAt: string | null
}

export interface AdminOrderItem {
  cardId: number
  cardSetId: string
  cardName: string
  cardNameEn: string
  imageUrl: string | null
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface AdminOrderPayment {
  status: string
  cardLast4: string | null
  failureCode: string | null
  createdAt: string
}

export interface AdminOrder {
  orderNo: string
  status: OrderStatus
  currency: string
  subtotal: number
  shippingFee: number
  total: number
  customerId: number | null
  customerName: string
  customerEmail: string
  customerPhone: string
  recipientName: string
  recipientPhone: string
  postalCode: string
  city: string
  address: string
  cancelReason: string | null
  createdAt: string
  paidAt: string | null
  shippedAt: string | null
  completedAt: string | null
  cancelledAt: string | null
  updatedAt: string
  trackingNo: string | null
  staffNote: string | null
  items: AdminOrderItem[]
  payment: AdminOrderPayment | null
}
