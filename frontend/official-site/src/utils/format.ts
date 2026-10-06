import { i18n } from '@/i18n'
import type { Flag } from '@/types/card'

export function formatPrice(value: number): string {
  return `US$ ${value.toFixed(2)}`
}

// 有庫存、已定價且所屬系列上架才可買
export function isAvailable(card: { stock: number; salePrice: number; onSale: Flag }): boolean {
  return card.stock > 0 && card.salePrice > 0 && card.onSale === 1
}

// 系列下架，仍顯示但不販售
export function isOffShelf(card: { onSale: Flag }): boolean {
  return card.onSale === 0
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
