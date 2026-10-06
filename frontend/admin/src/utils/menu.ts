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

// 選單中所有節點的代碼，沒有頁面路徑的節點代表某項功能權限
export function allCodes(menu: MenuNode[]): string[] {
  return menu.flatMap((n) => [n.code, ...allCodes(n.children)])
}

// 頁面用 inject 取得，判斷是否顯示某個操作按鈕
export const MENU_CODES = Symbol('menuCodes')
