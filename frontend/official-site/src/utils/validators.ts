// 規則與後端一致

export const isEmail = (s: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(s.trim())

// 帳號：3 到 30 個英數字、點、底線或連字號，不分大小寫
export const isUsername = (s: string) => /^[a-z0-9][a-z0-9._-]{2,29}$/i.test(s.trim())

export const isPhone = (s: string) => /^[0-9+\-() ]{8,20}$/.test(s.trim())

export const isPostalCode = (s: string) => /^[0-9]{3}([0-9]{2,3})?$/.test(s.trim())

// 密碼規則與後端一致：至少 8 個字元，需含英文字母與數字，最多 72 個位元組
export const isPasswordLongEnough = (s: string) => s.length >= 8

export const isPasswordMixed = (s: string) => /[A-Za-z]/.test(s) && /\d/.test(s)

export const isPasswordTooLong = (s: string) => new TextEncoder().encode(s).length > 72
