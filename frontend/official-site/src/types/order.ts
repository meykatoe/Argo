export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'SHIPPED' | 'COMPLETED'

export interface OrderItem {
  cardId: number
  cardSetId: string
  cardName: string
  cardNameEn: string
  imageUrl: string | null
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface PaymentInfo {
  status: string
  cardLast4: string | null
  failureCode: string | null
  createdAt: string
}

export interface Order {
  orderNo: string
  status: OrderStatus
  currency: string
  subtotal: number
  shippingFee: number
  total: number
  createdAt: string
  expiresAt: string
  paidAt: string | null
  shippedAt: string | null
  trackingNo: string | null
  cancelReason: string | null
  customerName: string
  customerEmail: string
  recipientName: string
  recipientPhone: string
  postalCode: string
  city: string
  address: string
  items: OrderItem[]
  payment: PaymentInfo | null
}

export interface CreateOrderBody {
  items: { cardId: number; quantity: number }[]
  customer: { name: string; email: string; phone: string }
  shipping: {
    recipientName: string
    recipientPhone: string
    postalCode: string
    city: string
    address: string
  }
}

export interface PayBody {
  email: string
  card: {
    number: string
    expMonth: number
    expYear: number
    cvc: string
    holderName: string
  }
}
