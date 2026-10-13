import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import SystemMonitor from '@/components/SystemMonitor.vue'
import type { Monitor } from '@/types'

function monitor(over: Partial<Monitor> = {}): Monitor {
  return {
    level: 'NORMAL',
    windowMinutes: 5,
    system: {
      cpuPercent: 12.5,
      heapPercent: 40,
      heapUsedMb: 400,
      heapMaxMb: 1000,
      threads: 52,
      poolActive: 2,
      poolMax: 10,
      poolPending: 0,
      poolPercent: 20,
      gcPercent: 0.4,
      uptimeSeconds: 7500,
    },
    apps: [
      { app: 'official-site', requests: 1200, perMinute: 240, errors5xx: 6, errorRate: 0.5, avgMs: 35, p95Ms: 120, p99Ms: 400, maxMs: 2500 },
      { app: 'admin', requests: 300, perMinute: 60, errors5xx: 0, errorRate: 0, avgMs: 20, p95Ms: 60, p99Ms: 90, maxMs: 150 },
    ],
    slowest: [{ app: 'official-site', method: 'GET', path: '/api/cards', requests: 800, errors5xx: 0, errorRate: 0, avgMs: 60, p95Ms: 300, maxMs: 2500 }],
    failing: [],
    alerts: [],
    history: [
      { at: 1_000_000, cpu: 10, heap: 38, pool: 20, gc: 0.2 },
      { at: 1_010_000, cpu: 12.5, heap: 40, pool: 20, gc: 0.4 },
    ],
    ...over,
  }
}

function reply(data: Monitor) {
  const fn = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data }) })
  vi.stubGlobal('fetch', fn)
  return fn
}

afterEach(() => vi.unstubAllGlobals())

describe('SystemMonitor', () => {
  it('帶令牌與時間範圍載入，顯示資源與各前端數字', async () => {
    const fn = reply(monitor())
    const w = mount(SystemMonitor, { props: { token: 'tk' } })
    await flushPromises()
    const [url, init] = fn.mock.calls[0]!
    expect(url).toContain('/ops/monitor')
    expect(url).toContain('minutes=5')
    expect(init.headers['Authorization']).toBe('Bearer tk')
    const text = w.text()
    expect(text).toContain('目前沒有預警，系統負載正常')
    expect(text).toContain('12.5%')
    expect(text).toContain('400 / 1000 MB')
    expect(text).toContain('使用 2 / 10，等待 0')
    expect(text).toContain('官網')
    expect(text).toContain('業務後台')
    expect(text).toContain('1,200')
    expect(text).toContain('120 ms')
    expect(text).toContain('GET /api/cards')
    expect(text).toContain('這段時間沒有伺服器錯誤')
  })

  it('有預警時列出原因，並在對應的卡片標出等級', async () => {
    reply(
      monitor({
        level: 'DANGER',
        alerts: [
          { code: 'DB_POOL_WAITING', level: 'DANGER', app: null, value: 3, threshold: 1 },
          { code: 'LATENCY_HIGH', level: 'WARNING', app: 'official-site', value: 2500, threshold: 2000 },
        ],
      }),
    )
    const w = mount(SystemMonitor, { props: { token: 'tk' } })
    await flushPromises()
    expect(w.text()).toContain('有 2 項預警')
    expect(w.text()).toContain('有請求在等資料庫連線，目前 3 個（門檻 1 個）')
    expect(w.text()).toContain('官網：回應偏慢（p95），目前 2500 ms（門檻 2000 ms）')
    const tiles = w.findAll('.tile')
    expect(tiles[2]!.text()).toContain('危險')
    expect(tiles[0]!.text()).not.toContain('危險')
    expect(tiles[0]!.text()).not.toContain('警告')
  })

  it('切換時間範圍會重新載入', async () => {
    const fn = reply(monitor())
    const w = mount(SystemMonitor, { props: { token: 'tk' } })
    await flushPromises()
    await w.find('select').setValue(60)
    await flushPromises()
    expect(fn.mock.calls[1]![0]).toContain('minutes=60')
  })

  it('有未標示來源的請求時說明它是什麼', async () => {
    reply(
      monitor({
        apps: [{ app: 'unknown', requests: 9, perMinute: 2, errors5xx: 0, errorRate: 0, avgMs: 5, p95Ms: 10, p99Ms: 10, maxMs: 12 }],
      }),
    )
    const w = mount(SystemMonitor, { props: { token: 'tk' } })
    await flushPromises()
    expect(w.text()).toContain('未標示來源')
    expect(w.text()).toContain('沒有帶前端標頭')
  })

  it('沒有任何請求時顯示空白提示', async () => {
    reply(monitor({ apps: [], slowest: [] }))
    const w = mount(SystemMonitor, { props: { token: 'tk' } })
    await flushPromises()
    expect(w.text()).toContain('這段時間沒有請求紀錄')
    expect(w.text()).toContain('這段時間沒有資料')
  })

  it('提供資源取樣的表格檢視', async () => {
    reply(monitor())
    const w = mount(SystemMonitor, { props: { token: 'tk' } })
    await flushPromises()
    const raw = w.find('details.raw')
    expect(raw.text()).toContain('最近 2 筆')
    expect(raw.findAll('tbody tr')).toHaveLength(2)
  })

  it('401 時通知外層登出', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({ code: 401, msg: 'UNAUTHORIZED', data: null }) }))
    const w = mount(SystemMonitor, { props: { token: 'tk' } })
    await flushPromises()
    expect(w.emitted('unauthorized')).toHaveLength(1)
  })

  it('載入失敗顯示錯誤而不是空白', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 500, json: async () => ({ code: 500, msg: 'INTERNAL_ERROR', data: null }) }))
    const w = mount(SystemMonitor, { props: { token: 'tk' } })
    await flushPromises()
    expect(w.find('[role=alert]').exists()).toBe(true)
  })
})
