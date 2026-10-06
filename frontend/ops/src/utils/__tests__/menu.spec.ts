import { describe, expect, it } from 'vitest'
import type { MenuNode } from '@/types'
import { allCodes, firstPath, pageNodes, trailOf } from '../menu'

const menu: MenuNode[] = [
  {
    code: 'card',
    title: '卡牌管理',
    path: null,
    children: [
      { code: 'card.series', title: '卡牌系列', path: '/series', children: [] },
      { code: 'card.edit', title: '卡牌編輯', path: '/cards', children: [] },
    ],
  },
  { code: 'solo', title: '單一頁', path: '/solo', children: [] },
]

describe('menu utils', () => {
  it('依序列出所有可開啟的頁面', () => {
    expect(pageNodes(menu).map((n) => n.path)).toEqual(['/series', '/cards', '/solo'])
    expect(firstPath(menu)).toBe('/series')
    expect(firstPath([])).toBeNull()
  })

  it('取得從群組到頁面的路徑', () => {
    expect(trailOf(menu, '/cards').map((n) => n.title)).toEqual(['卡牌管理', '卡牌編輯'])
    expect(trailOf(menu, '/solo').map((n) => n.title)).toEqual(['單一頁'])
    expect(trailOf(menu, '/nope')).toEqual([])
  })

  it('列出所有節點代碼，包含沒有頁面的功能權限', () => {
    const m: MenuNode[] = [
      {
        code: 'security',
        title: '安全管理',
        path: null,
        children: [
          { code: 'security.ips', title: 'IP', path: '/ips', children: [] },
          { code: 'security.ips.block', title: '封鎖', path: null, children: [] },
        ],
      },
    ]
    expect(allCodes(m)).toEqual(['security', 'security.ips', 'security.ips.block'])
    expect(pageNodes(m).map((n) => n.code)).toEqual(['security.ips'])
    expect(firstPath(m)).toBe('/ips')
  })
})
