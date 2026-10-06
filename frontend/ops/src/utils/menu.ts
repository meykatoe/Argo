import type { MenuNode } from '@/types'

// 所有可開啟頁面的節點，依選單順序
export function pageNodes(menu: MenuNode[]): MenuNode[] {
  return menu.flatMap((n) => [...(n.path ? [n] : []), ...pageNodes(n.children)])
}

export function firstPath(menu: MenuNode[]): string | null {
  return pageNodes(menu)[0]?.path ?? null
}

// 從根到目前頁面的節點，用於麵包屑
export function trailOf(menu: MenuNode[], path: string): MenuNode[] {
  for (const n of menu) {
    if (n.path === path) {
      return [n]
    }
    const inner = trailOf(n.children, path)
    if (inner.length > 0) {
      return [n, ...inner]
    }
  }
  return []
}
