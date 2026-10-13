import { describe, expect, it } from 'vitest'
import type { MonitorAlert } from '@/types'
import {
  alertText,
  appColor,
  appLabel,
  formatMs,
  formatUptime,
  nearestIndex,
  toDots,
  worstLevel,
} from '@/utils/monitor'

const alert = (over: Partial<MonitorAlert>): MonitorAlert => ({
  code: 'CPU_HIGH',
  level: 'WARNING',
  app: null,
  value: 90,
  threshold: 85,
  ...over,
})

describe('前端名稱與顏色', () => {
  it('每個前端有固定名稱與顏色', () => {
    expect(appLabel('official-site')).toBe('官網')
    expect(appLabel('admin')).toBe('業務後台')
    expect(appLabel('ops')).toBe('運維後台')
    expect(appLabel('unknown')).toBe('未標示來源')
    expect(appColor('official-site')).not.toBe(appColor('admin'))
  })

  it('沒見過的名稱原樣顯示並用灰色', () => {
    expect(appLabel('x')).toBe('x')
    expect(appColor('x')).toBe(appColor('unknown'))
  })
})

describe('預警文字', () => {
  it('資源預警說明目前值與門檻', () => {
    expect(alertText(alert({}))).toBe('CPU 使用率偏高，目前 90%（門檻 85%）')
  })

  it('前端預警前面加上前端名稱', () => {
    const t = alertText(alert({ code: 'LATENCY_HIGH', app: 'official-site', value: 2500, threshold: 2000 }))
    expect(t).toBe('官網：回應偏慢（p95），目前 2500 ms（門檻 2000 ms）')
  })

  it('沒見過的代碼不會壞掉', () => {
    expect(alertText(alert({ code: 'NEW_ONE' }))).toContain('NEW_ONE')
  })

  it('取指定代碼中最嚴重的等級', () => {
    const list = [alert({ code: 'DB_POOL_HIGH' }), alert({ code: 'DB_POOL_WAITING', level: 'DANGER' }), alert({ code: 'CPU_HIGH' })]
    expect(worstLevel(list, ['DB_POOL_HIGH', 'DB_POOL_WAITING'])).toBe('DANGER')
    expect(worstLevel(list, ['CPU_HIGH'])).toBe('WARNING')
    expect(worstLevel(list, ['HEAP_HIGH'])).toBe('NORMAL')
  })
})

describe('格式', () => {
  it('毫秒與秒', () => {
    expect(formatMs(35.5)).toBe('35.5 ms')
    expect(formatMs(2500)).toBe('2.5 s')
  })

  it('運行時間', () => {
    expect(formatUptime(90)).toBe('1 分鐘')
    expect(formatUptime(3 * 3600 + 5 * 60)).toBe('3 小時 5 分')
    expect(formatUptime(2 * 86400 + 4 * 3600)).toBe('2 天 4 小時')
  })
})

describe('圖上座標', () => {
  it('指定上限時 0 在底部、上限在頂部', () => {
    const [low, high] = toDots([0, 100], 240, 56, 6, 100)
    expect(low!.y).toBe(50)
    expect(high!.y).toBe(6)
    expect(low!.x).toBe(6)
    expect(high!.x).toBe(234)
  })

  it('超出範圍的值會被限制在圖內', () => {
    const [over, under] = toDots([500, -5], 240, 56, 6, 100)
    expect(over!.y).toBe(6)
    expect(under!.y).toBe(50)
  })

  it('沒指定上限就依資料縮放，全為 0 也不會除以零', () => {
    const dots = toDots([0, 0, 0], 240, 56, 6)
    expect(dots.every((d) => d.y === 50)).toBe(true)
    const scaled = toDots([1, 2], 240, 56, 6)
    expect(scaled[1]!.y).toBeLessThan(scaled[0]!.y)
  })

  it('只有一筆時放在中間', () => {
    expect(toDots([5], 240, 56, 6, 10)[0]!.x).toBe(120)
  })

  it('游標對應最近的資料點並限制在範圍內', () => {
    expect(nearestIndex(6, 5, 240, 6)).toBe(0)
    expect(nearestIndex(234, 5, 240, 6)).toBe(4)
    expect(nearestIndex(120, 5, 240, 6)).toBe(2)
    expect(nearestIndex(-50, 5, 240, 6)).toBe(0)
    expect(nearestIndex(999, 5, 240, 6)).toBe(4)
    expect(nearestIndex(10, 1, 240, 6)).toBe(0)
  })
})
