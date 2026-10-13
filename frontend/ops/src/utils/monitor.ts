import type { MonitorAlert, MonitorLevel } from '@/types'

const APP_LABEL: Record<string, string> = {
  'official-site': '官網',
  admin: '業務後台',
  ops: '運維後台',
  unknown: '未標示來源',
}

// 依前端固定
const APP_COLOR: Record<string, string> = {
  'official-site': '#2a78d6',
  admin: '#eb6834',
  ops: '#1baf7a',
  unknown: '#898781',
}

export const appLabel = (app: string): string => APP_LABEL[app] ?? app
export const appColor = (app: string): string => APP_COLOR[app] ?? '#898781'

export const LEVEL_LABEL: Record<MonitorLevel, string> = {
  NORMAL: '正常',
  WARNING: '警告',
  DANGER: '危險',
}

const ALERTS: Record<string, { title: string; unit: string }> = {
  DB_POOL_HIGH: { title: '資料庫連線池使用率偏高', unit: '%' },
  DB_POOL_WAITING: { title: '有請求在等資料庫連線', unit: ' 個' },
  HEAP_HIGH: { title: '記憶體使用率偏高', unit: '%' },
  CPU_HIGH: { title: 'CPU 使用率偏高', unit: '%' },
  LATENCY_HIGH: { title: '回應偏慢（p95）', unit: ' ms' },
  ERROR_RATE_HIGH: { title: '伺服器錯誤率偏高', unit: '%' },
}

export function alertText(a: MonitorAlert): string {
  const meta = ALERTS[a.code] ?? { title: a.code, unit: '' }
  const who = a.app ? `${appLabel(a.app)}：` : ''
  return `${who}${meta.title}，目前 ${a.value}${meta.unit}（門檻 ${a.threshold}${meta.unit}）`
}

const RANK: Record<MonitorLevel, number> = { NORMAL: 0, WARNING: 1, DANGER: 2 }

// 最嚴重等級
export function worstLevel(alerts: MonitorAlert[], codes: string[]): MonitorLevel {
  let worst: MonitorLevel = 'NORMAL'
  for (const a of alerts) {
    if (codes.includes(a.code) && RANK[a.level] > RANK[worst]) worst = a.level
  }
  return worst
}

export function formatMs(ms: number): string {
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)} s` : `${ms} ms`
}

export function formatUptime(seconds: number): string {
  const m = Math.floor(seconds / 60)
  if (m < 60) return `${m} 分鐘`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} 小時 ${m % 60} 分`
  return `${Math.floor(h / 24)} 天 ${h % 24} 小時`
}

export const clockTime = (ms: number): string =>
  new Date(ms).toLocaleTimeString('zh-TW', { hour12: false })

export interface Dot {
  x: number
  y: number
}

// 轉成座標
export function toDots(values: number[], w: number, h: number, pad: number, max?: number): Dot[] {
  const top = max ?? Math.max(Math.max(...values, 0) * 1.2, 1)
  const n = values.length
  return values.map((v, i) => ({
    x: n === 1 ? w / 2 : pad + (i / (n - 1)) * (w - 2 * pad),
    y: h - pad - (Math.min(Math.max(v, 0), top) / top) * (h - 2 * pad),
  }))
}

// 找最近的點
export function nearestIndex(x: number, n: number, w: number, pad: number): number {
  if (n <= 1) return 0
  const i = Math.round(((x - pad) / (w - 2 * pad)) * (n - 1))
  return Math.min(Math.max(i, 0), n - 1)
}
