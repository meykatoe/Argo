// 規則與後端一致

export const isEmail = (s: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(s.trim())

export const isPhone = (s: string) => /^[0-9+\-() ]{8,20}$/.test(s.trim())

export const isPostalCode = (s: string) => /^[0-9]{3}([0-9]{2,3})?$/.test(s.trim())

// 卡號檢查碼
export function luhn(digits: string): boolean {
  let sum = 0
  let double = false
  for (let i = digits.length - 1; i >= 0; i--) {
    let d = digits.charCodeAt(i) - 48
    if (double) {
      d *= 2
      if (d > 9) d -= 9
    }
    sum += d
    double = !double
  }
  return sum % 10 === 0
}

export function normalizeCardNumber(s: string): string {
  return s.replace(/[ -]/g, '')
}

export function isCardNumber(s: string): boolean {
  const d = normalizeCardNumber(s)
  return /^[0-9]{13,19}$/.test(d) && luhn(d)
}

export function isCardExpired(month: number, year: number, now = new Date()): boolean {
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  return year < y || (year === y && month < m)
}

export const isCvc = (s: string) => /^[0-9]{3,4}$/.test(s)

// 每四碼加空白，方便輸入
export function formatCardNumber(s: string): string {
  return normalizeCardNumber(s)
    .replace(/[^0-9]/g, '')
    .slice(0, 19)
    .replace(/(.{4})/g, '$1 ')
    .trim()
}
