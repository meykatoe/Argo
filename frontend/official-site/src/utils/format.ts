import { i18n } from '@/i18n'

export function formatPrice(value: number): string {
  return `US$ ${value.toFixed(2)}`
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
