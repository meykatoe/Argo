import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { REFRESH_MS, useAutoRefresh } from '../autoRefresh'

function hidden(value: boolean) {
  Object.defineProperty(document, 'hidden', { configurable: true, get: () => value })
}

function host(tick: () => Promise<void> | void, canRun?: () => boolean) {
  let api!: ReturnType<typeof useAutoRefresh>
  const C = defineComponent({
    setup() {
      api = useAutoRefresh(tick, canRun)
      return () => h('div')
    },
  })
  const w = mount(C)
  return { w, api }
}

beforeEach(() => {
  localStorage.clear()
  hidden(false)
  vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] })
})
afterEach(() => {
  vi.useRealTimers()
  hidden(false)
})

describe('useAutoRefresh', () => {
  it('預設開啟，每 10 秒執行一次', async () => {
    const tick = vi.fn()
    host(tick)
    vi.advanceTimersByTime(REFRESH_MS - 1)
    expect(tick).not.toHaveBeenCalled()
    vi.advanceTimersByTime(1)
    expect(tick).toHaveBeenCalledTimes(1)
    // 每次都要等上一次做完，所以逐次推進
    for (let i = 0; i < 2; i++) {
      await flushPromises()
      vi.advanceTimersByTime(REFRESH_MS)
    }
    expect(tick).toHaveBeenCalledTimes(3)
  })

  it('關閉後不再執行，且記住選擇', async () => {
    const tick = vi.fn()
    const { api } = host(tick)
    api.enabled.value = false
    await flushPromises()
    vi.advanceTimersByTime(REFRESH_MS * 3)
    expect(tick).not.toHaveBeenCalled()
    expect(localStorage.getItem('argo.ops.autoRefresh')).toBe('0')
    const again = host(tick)
    expect(again.api.enabled.value).toBe(false)
  })

  it('分頁在背景時略過，回到前景立刻更新一次', async () => {
    const tick = vi.fn()
    host(tick)
    hidden(true)
    vi.advanceTimersByTime(REFRESH_MS * 2)
    expect(tick).not.toHaveBeenCalled()
    hidden(false)
    document.dispatchEvent(new Event('visibilitychange'))
    await flushPromises()
    expect(tick).toHaveBeenCalledTimes(1)
  })

  it('頁面說現在不適合時略過', () => {
    const tick = vi.fn()
    host(tick, () => false)
    vi.advanceTimersByTime(REFRESH_MS * 2)
    expect(tick).not.toHaveBeenCalled()
  })

  it('上一次還沒做完就不會重疊執行', async () => {
    let release!: () => void
    const tick = vi.fn(() => new Promise<void>((r) => (release = r)))
    host(tick)
    vi.advanceTimersByTime(REFRESH_MS)
    vi.advanceTimersByTime(REFRESH_MS)
    expect(tick).toHaveBeenCalledTimes(1)
    release()
    await flushPromises()
    vi.advanceTimersByTime(REFRESH_MS)
    expect(tick).toHaveBeenCalledTimes(2)
  })

  it('離開頁面後停止', () => {
    const tick = vi.fn()
    const { w } = host(tick)
    w.unmount()
    vi.advanceTimersByTime(REFRESH_MS * 3)
    expect(tick).not.toHaveBeenCalled()
  })

  it('執行失敗不會讓定時器停掉', async () => {
    const tick = vi.fn().mockRejectedValueOnce(new Error('x')).mockResolvedValue(undefined)
    host(() => tick().catch(() => undefined))
    vi.advanceTimersByTime(REFRESH_MS)
    await flushPromises()
    vi.advanceTimersByTime(REFRESH_MS)
    expect(tick).toHaveBeenCalledTimes(2)
  })
})
