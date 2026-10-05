import { i18n } from '@/i18n'

export function formatPrice(value: number): string {
  return `US$ ${value.toFixed(2)}`
}

// 有庫存且已定價才可買
export function isAvailable(card: { stock: number; salePrice: number }): boolean {
  return card.stock > 0 && card.salePrice > 0
}

// 多色卡以空白分隔
export function colorText(color: string): string {
  const { t, te } = i18n.global
  return color
    .split(' ')
    .map((c) => (te(`color.${c}`) ? t(`color.${c}`) : c))
    .join('/')
}

export function typeText(type: string): string {
  const { t, te } = i18n.global
  return te(`cardType.${type}`) ? t(`cardType.${type}`) : type
}

const SYMBOLS: Record<string, string> = { USD: 'US$', TWD: 'NT$' }

// 依訂單幣別顯示金額
export function formatMoney(value: number, currency: string): string {
  return `${SYMBOLS[currency] ?? currency} ${value.toFixed(2)}`
}
