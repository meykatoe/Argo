// 解析折扣輸入，範圍 (0, 1]，最多四位小數
export function parseDiscount(input: string): number | null {
  const text = input.trim()
  if (!/^\d(\.\d{1,4})?$/.test(text)) {
    return null
  }
  const n = Number(text)
  return n > 0 && n <= 1 ? n : null
}

// 0.4 顯示為 4 折，1 顯示為無折扣
export function discountLabel(value: number): string {
  if (value >= 1) {
    return '無折扣'
  }
  return `${Number((value * 10).toFixed(3))} 折`
}

export function formatPrice(value: number): string {
  return `US$ ${value.toFixed(2)}`
}
