// 只接受站內路徑，避免被導到外部網站
export function safeRedirect(value: unknown, fallback = '/'): string {
  if (typeof value !== 'string') return fallback
  if (!value.startsWith('/') || value.startsWith('//') || value.includes('\\')) return fallback
  return value
}
