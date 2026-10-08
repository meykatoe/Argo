import type { Component } from 'vue'
import type { ErrorTexts } from './utils/error'
import type { MenuNode, Session } from './types'

// 後台差異都由各專案用這個設定交給共用外殼
export interface StaffConfig {
  // 登入資料在 sessionStorage 的鍵
  storageKey: string
  // 側邊欄顯示的後台名稱
  brand: string
  // 登入頁標題
  title: string
  // 選單路徑對應的頁面
  pages: Record<string, Component>
  errors: ErrorTexts
  api: {
    login: (username: string, password: string) => Promise<Session>
    logout: (token: string) => Promise<unknown>
    getMenu: (token: string) => Promise<MenuNode[]>
  }
}
